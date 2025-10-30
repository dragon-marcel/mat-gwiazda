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
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.domain.entity.LearningLevel;
import pl.matgwiazda.domain.entity.Progress;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.TaskDto;
import pl.matgwiazda.dto.TaskGenerateCommand;
import pl.matgwiazda.dto.TaskWithProgressDto;
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

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final ProgressRepository progressRepository;
    private final TransactionTemplate txTemplate;
    private final LearningLevelRepository learningLevelRepository;
    private final OpenRouterService openRouterService;

    @Autowired
    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TaskMapper taskMapper, ProgressRepository progressRepository, PlatformTransactionManager txManager, LearningLevelRepository learningLevelRepository, OpenRouterService openRouterService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
        this.progressRepository = progressRepository;
        this.txTemplate = new TransactionTemplate(txManager);
        this.learningLevelRepository = learningLevelRepository;
        this.openRouterService = openRouterService;
    }

    // Public wrapper with retry logic using TransactionTemplate
    public TaskWithProgressDto generateTask(TaskGenerateCommand cmd, UUID userId) {
        final int maxAttempts = 3;
        int attempt = 0;

        // Fetch learning level early (avoid holding DB locks while calling external API)
        LearningLevel learningLevel = null;
        if (cmd != null && cmd.getLevel() != null) {
            learningLevel = learningLevelRepository.findById(cmd.getLevel()).orElse(null);
        }

        while (true) {
            attempt++;
            try {
                final LearningLevel finalLearningLevel = learningLevel; // effectively final for lambda
                return txTemplate.execute((TransactionCallback<TaskWithProgressDto>) status -> {
                    // Validate user
                    User user = null;
                    if (userId != null) {
                        // Lock the user row first to establish consistent lock ordering (user -> progress)
                        user = userRepository.findByIdForUpdate(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
                        // if user already has an active progress, return existing task+progress
                        if (user.getActiveProgressId() != null) {
                            UUID existingProgressId = user.getActiveProgressId();
                            Optional<Progress> existingProgress = progressRepository.findById(existingProgressId);
                            if (existingProgress.isPresent()) {
                                Task existingTask = existingProgress.get().getTask();
                                return new TaskWithProgressDto(taskMapper.toDto(existingTask), existingProgressId);
                            }
                        }
                    }

                    // Resolve createdBy if provided
                    User createdBy = null;
                    if (cmd.getCreatedById() != null) {
                        createdBy = userRepository.findById(cmd.getCreatedById())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "`createdById` does not reference an existing user"));
                    }

                    // Attempt AI generation if we have a learning level template
                    AiTaskResult aiResult = null;
                    if (finalLearningLevel != null) {
                        try {
                            // use single-arg overload (seed from learningLevel.description)
                            aiResult = openRouterService.generateTaskFromSeed(finalLearningLevel.getDescription());
                        } catch (OpenRouterException ex) {
                            log.warn("OpenRouter generation failed: {}", ex.getMessage());
                            // do not rethrow - fallback path below
                        }
                    }

                    Task task = null;
                    if (aiResult != null) {
                        // Map AI result to Task entity
                        task = new Task();
                        task.setLevel(cmd.getLevel());
                        task.setPrompt(aiResult.prompt());
                        // AiTaskResult.options() now returns List<String> - copy into new ArrayList to get a mutable list
                        task.setOptions(new ArrayList<>(aiResult.options()));
                        task.setCorrectOptionIndex((short) aiResult.correctIndex());
                        task.setExplanation(aiResult.explanation());
                        task.setCreatedBy(createdBy);
                        task.setActive(true);
                    }

                    // Persist generated task
                    Task savedTask = taskRepository.save(task);

                    // Create progress for this task and user
                    Progress progress = new Progress();
                    if (user != null) progress.setUser(user);
                    progress.setTask(savedTask);
                    progress.setAttemptNumber(1);
                    progress.setCorrect(false);
                    progress.setPointsAwarded(0);
                    progress.setFinalized(false);
                    // Persist progress first
                    Progress savedProgress = progressRepository.save(progress);

                    // Update user's activeProgressId while still holding the user lock (if user was locked)
                    if (user != null) {
                        user.setActiveProgressId(savedProgress.getId());
                        userRepository.save(user);
                    }

                    return new TaskWithProgressDto(taskMapper.toDto(savedTask), savedProgress.getId());
                });
            } catch (ConcurrencyFailureException ex) {
                if (attempt >= maxAttempts) throw ex;
                try { Thread.sleep(50L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

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
        Page<Task> page;
        if (level != null && createdById != null && isActive != null) {
            page = taskRepository.findByLevelAndCreatedByIdAndIsActive(level, createdById, isActive, pageable);
        } else if (level != null && createdById != null) {
            page = taskRepository.findByLevelAndCreatedById(level, createdById, pageable);
        } else if (level != null && isActive != null) {
            page = taskRepository.findByLevelAndIsActive(level, isActive, pageable);
        } else if (createdById != null && isActive != null) {
            page = taskRepository.findByCreatedByIdAndIsActive(createdById, isActive, pageable);
        } else if (level != null) {
            page = taskRepository.findByLevel(level, pageable);
        } else if (createdById != null) {
            page = taskRepository.findByCreatedById(createdById, pageable);
        } else if (isActive != null) {
            page = taskRepository.findByIsActive(isActive, pageable);
        } else {
            page = taskRepository.findAll(pageable);
        }

        return page.map(taskMapper::toDto);
    }

}
