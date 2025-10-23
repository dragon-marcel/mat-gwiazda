package pl.matgwiazda.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.domain.entity.Progress;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.ProgressDto;
import pl.matgwiazda.dto.ProgressSubmitCommand;
import pl.matgwiazda.dto.ProgressSubmitResponseDto;
import pl.matgwiazda.repository.ProgressRepository;
import pl.matgwiazda.repository.TaskRepository;
import pl.matgwiazda.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProgressService {

    private static final Logger log = LoggerFactory.getLogger(ProgressService.class);

    private final ProgressRepository progressRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Autowired
    public ProgressService(ProgressRepository progressRepository, TaskRepository taskRepository, UserRepository userRepository) {
        this.progressRepository = progressRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }
    /**
     * List ALL progress attempts for a user (no pagination).
     * Returns all progress entries for the user without filters.
     */
    public List<ProgressDto> listAllProgress(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        List<Progress> results = progressRepository.findByUserId(userId, Sort.unsorted());
        return results.stream().map(this::toDto).toList();
    }

    /**
     * Submit progress for a user attempting a task.
     * This method orchestrates smaller responsibilities and is transactional so any exception will rollback changes.
     */
    @Transactional(rollbackFor = Exception.class)
    public ProgressSubmitResponseDto submitProgress(UUID userId, ProgressSubmitCommand cmd) {

        log.debug("submitProgress start: userId={}, taskId={}, selectedOptionIndex={}", userId, cmd.getTaskId(), cmd.getSelectedOptionIndex());

        User user = findUserOrThrow(userId);
        Task task = findTaskOrThrow(cmd.getTaskId());

        boolean isCorrect = determineCorrectness(cmd.getSelectedOptionIndex(), task.getCorrectOptionIndex());
        int pointsAwarded = isCorrect ? 1 : 0;

        // Update user stats and persist
        updateUserStats(user, pointsAwarded);
        userRepository.save(user);

        // Upsert progress
        Progress savedProgress = upsertProgress(user, task, cmd, isCorrect, pointsAwarded);

        // Prepare and return response
        ProgressSubmitResponseDto response = buildResponse(savedProgress, user, pointsAwarded, isCorrect);
        log.info("submitProgress completed: userId={}, taskId={}, progressId={}, isCorrect={}", userId, cmd.getTaskId(), savedProgress.getId(), isCorrect);
        return response;
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
    }

    private Task findTaskOrThrow(UUID taskId) {
        Optional<Task> taskOpt = taskRepository.findByIdAndIsActive(taskId, true);
        return taskOpt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found or not active"));
    }

    private boolean determineCorrectness(Short selectedOptionIndex, short correctIndex) {
        return selectedOptionIndex != null && selectedOptionIndex == correctIndex;
    }

    private void updateUserStats(User user, int pointsAwarded) {
        int newUserPoints = user.getPoints() + pointsAwarded;
        int previousThresholdCount = user.getPoints() / 50;
        int newThresholdCount = newUserPoints / 50;
        int levelsGained = Math.max(0, newThresholdCount - previousThresholdCount);
        int starsAwarded = levelsGained;

        user.setPoints(newUserPoints);
        user.setStars(user.getStars() + starsAwarded);
        user.setCurrentLevel((short) (user.getCurrentLevel() + levelsGained));
    }

    private Progress upsertProgress(User user, Task task, ProgressSubmitCommand cmd, boolean isCorrect, int pointsAwarded) {
        // mark task active flag according to correctness (business rule contained here)
        task.setActive(!isCorrect);

        Optional<Progress> existing = progressRepository.findByUserIdAndTaskId(user.getId(), task.getId());
        Progress progress = existing.orElseGet(Progress::new);
        progress.setUser(user);
        progress.setTask(task);
        progress.setAttemptNumber(progress.getAttemptNumber() + 1);
        progress.setSelectedOptionIndex(cmd.getSelectedOptionIndex());
        progress.setCorrect(isCorrect);
        progress.setPointsAwarded(pointsAwarded);
        progress.setTimeTakenMs(cmd.getTimeTakenMs());

        return progressRepository.save(progress);
    }

    private ProgressSubmitResponseDto buildResponse(Progress saved, User user, int pointsAwarded, boolean isCorrect) {
        String explanation = saved.getTask() != null ? saved.getTask().getExplanation() : null;
        return new ProgressSubmitResponseDto(saved.getId(), isCorrect, pointsAwarded, user.getPoints(), 0, false, user.getCurrentLevel(), explanation);
    }

    private ProgressDto toDto(Progress p) {
        ProgressDto dto = new ProgressDto();
        dto.setId(p.getId());
        dto.setUserId(p.getUser() != null ? p.getUser().getId() : null);
        dto.setTaskId(p.getTask() != null ? p.getTask().getId() : null);
        dto.setAttemptNumber(p.getAttemptNumber());
        dto.setSelectedOptionIndex(p.getSelectedOptionIndex());
        dto.setCorrect(p.isCorrect());
        dto.setPointsAwarded(p.getPointsAwarded());
        dto.setTimeTakenMs(p.getTimeTakenMs());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}
