package pl.matgwiazda.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.domain.entity.LearningLevel;
import pl.matgwiazda.domain.entity.Progress;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.TaskDto;
import pl.matgwiazda.dto.TaskGenerateCommand;
import pl.matgwiazda.dto.TaskWithProgressDto;
import pl.matgwiazda.dto.openrouter.AiTaskResult;
import pl.matgwiazda.exception.OpenRouterException;
import pl.matgwiazda.mapper.TaskMapper;
import pl.matgwiazda.repository.LearningLevelRepository;
import pl.matgwiazda.repository.ProgressRepository;
import pl.matgwiazda.repository.TaskRepository;
import pl.matgwiazda.repository.UserRepository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long BACKOFF_STEP_MS = 50L;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final ProgressRepository progressRepository;
    private final TransactionTemplate txTemplate;
    private final LearningLevelRepository learningLevelRepository;
    private final OpenRouterService openRouterService;
    private final ProgressService progressService;

    @Autowired
    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TaskMapper taskMapper, ProgressRepository progressRepository, PlatformTransactionManager txManager, LearningLevelRepository learningLevelRepository, OpenRouterService openRouterService, ProgressService progressService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
        this.progressRepository = progressRepository;
        this.txTemplate = new TransactionTemplate(txManager);
        this.learningLevelRepository = learningLevelRepository;
        this.openRouterService = openRouterService;
        this.progressService = progressService;
    }

    /**
     * Generate a task for a user (with retry on concurrency failures).
     */
    public TaskWithProgressDto generateTask(TaskGenerateCommand cmd, UUID userId) {
        int attempt = 0;

        // Fetch learning level early (avoid holding DB locks while calling external API)
        Optional<LearningLevel> learningLevel = fetchLearningLevel(cmd);

        while (true) {
            attempt++;
            try {
                final LearningLevel finalLearningLevel = learningLevel.orElse(null); // capture for tx
                return txTemplate.execute(status -> performGenerateTx(cmd, userId, finalLearningLevel));
            } catch (ConcurrencyFailureException ex) {
                if (attempt >= DEFAULT_MAX_ATTEMPTS) throw ex;
                sleepBackoff(attempt);
            }
        }
    }

    // Transactional worker method
    private TaskWithProgressDto performGenerateTx(TaskGenerateCommand cmd, UUID userId, LearningLevel finalLearningLevel) {
        // Validate and lock user (if provided)
        Optional<User> maybeUser = lockUserIfPresent(userId);

        // If user already has active progress, return it
        if (maybeUser.isPresent()) {
            Optional<TaskWithProgressDto> existing = getExistingActiveProgress(maybeUser.get());
            if (existing.isPresent()) return existing.get();
        }

        // Resolve createdBy (may throw)
        Optional<User> createdBy = resolveCreatedBy(cmd);

        // Create Task (may throw if generation failed)
        Task task = createTaskForCmd(cmd, finalLearningLevel, createdBy.orElse(null));

        Task savedTask = taskRepository.save(task);

        // Create and persist progress using ProgressService
        Progress progress = progressService.createInitialProgress(maybeUser.orElse(null), savedTask);
        Progress savedProgress = progressService.persistProgressAndUpdateUser(progress, maybeUser.orElse(null));

        return new TaskWithProgressDto(taskMapper.toDto(savedTask), savedProgress.getId());
    }

    private Task createTaskForCmd(TaskGenerateCommand cmd, LearningLevel finalLearningLevel, User createdBy) {
        // Attempt AI generation if we have a learning level template
        Optional<AiTaskResult> aiResult = tryGenerateAiTask(finalLearningLevel);

        Task task = aiResult.map(ai -> mapAiToTask(ai, cmd, createdBy)).orElse(null);

        if (task == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Task generation failed");
        }
        return task;
    }

    private Optional<LearningLevel> fetchLearningLevel(TaskGenerateCommand cmd) {
        if (cmd == null || cmd.getLevel() == null) return Optional.empty();
        return learningLevelRepository.findById(cmd.getLevel());
    }

    private Optional<User> lockUserIfPresent(UUID userId) {
        if (userId == null) return Optional.empty();
        return userRepository.findByIdForUpdate(userId);
    }

    private Optional<TaskWithProgressDto> getExistingActiveProgress(User user) {
        if (user.getActiveProgressId() == null) return Optional.empty();
        Optional<Progress> existingProgress = progressRepository.findById(user.getActiveProgressId());
        if (existingProgress.isPresent()) {
            Progress p = existingProgress.get();
            // If the progress was already finalized, clear the user's activeProgressId so a new task will be generated
            if (p.isFinalized()) {
                user.setActiveProgressId(null);
                try {
                    userRepository.save(user);
                } catch (Exception ex) {
                    // don't fail generation due to user save error; log if needed
                }
                return Optional.empty();
            }
            Task existingTask = p.getTask();
            return Optional.of(new TaskWithProgressDto(taskMapper.toDto(existingTask), p.getId()));
        }
        return Optional.empty();
    }

    private Optional<User> resolveCreatedBy(TaskGenerateCommand cmd) {
        if (cmd == null || cmd.getCreatedById() == null) return Optional.empty();
        Optional<User> opt = userRepository.findById(cmd.getCreatedById());
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`createdById` does not reference an existing user");
        }
        return opt;
    }

    private Optional<AiTaskResult> tryGenerateAiTask(LearningLevel finalLearningLevel) {
        if (finalLearningLevel == null) return Optional.empty();
        try {
            return Optional.ofNullable(openRouterService.generateTaskFromSeed(finalLearningLevel.getDescription()));
        } catch (OpenRouterException ex) {
            log.warn("OpenRouter generation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Task mapAiToTask(AiTaskResult aiResult, TaskGenerateCommand cmd, User createdBy) {
        Task task = taskMapper.fromAiResult(aiResult);
        // assign primitive short safely with default of 1 if command level is null
        short levelPrimitive = (cmd != null && cmd.getLevel() != null) ? cmd.getLevel() : (short) 1;
        task.setLevel(levelPrimitive);
        task.setCreatedBy(createdBy);
        task.setActive(true);
        if (task.getOptions() != null) {
            task.setOptions(new ArrayList<>(task.getOptions()));
        }
        return task;
    }

    private void sleepBackoff(int attempt) {
        try { Thread.sleep(BACKOFF_STEP_MS * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    @SuppressWarnings("unused")
    public TaskDto generateTask(TaskGenerateCommand cmd) {
        // keep legacy method for admin-like usage (no user binding)
        return generateTask(cmd, null).getTask();
    }

    public TaskDto getTaskById(UUID id) {
        Optional<Task> opt = taskRepository.findByIdAndIsActive(id, false);
        Task task = opt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        return taskMapper.toDto(task);
    }

    public Page<TaskDto> listTasks(Short level, Boolean isActive, UUID createdById, Pageable pageable) {
        Page<Task> page = findTasksPage(level, isActive, createdById, pageable);
        return page.map(taskMapper::toDto);
    }

    private Page<Task> findTasksPage(Short level, Boolean isActive, UUID createdById, Pageable pageable) {
        if (level != null && createdById != null && isActive != null) {
            return taskRepository.findByLevelAndCreatedByIdAndIsActive(level, createdById, isActive, pageable);
        } else if (level != null && createdById != null) {
            return taskRepository.findByLevelAndCreatedById(level, createdById, pageable);
        } else if (level != null && isActive != null) {
            return taskRepository.findByLevelAndIsActive(level, isActive, pageable);
        } else if (createdById != null && isActive != null) {
            return taskRepository.findByCreatedByIdAndIsActive(createdById, isActive, pageable);
        } else if (level != null) {
            return taskRepository.findByLevel(level, pageable);
        } else if (createdById != null) {
            return taskRepository.findByCreatedById(createdById, pageable);
        } else if (isActive != null) {
            return taskRepository.findByIsActive(isActive, pageable);
        } else {
            return taskRepository.findAll(pageable);
        }
    }

}
