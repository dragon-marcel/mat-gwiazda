package pl.matgwiazda.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
import java.util.UUID;

@Service
public class ProgressService {

    private static final Logger log = LoggerFactory.getLogger(ProgressService.class);

    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TransactionTemplate txTemplate;

    @Autowired
    public ProgressService(ProgressRepository progressRepository, UserRepository userRepository, TaskRepository taskRepository, PlatformTransactionManager txManager) {
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.txTemplate = new TransactionTemplate(txManager);
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
    // Public wrapper with retry logic using TransactionTemplate
    public ProgressSubmitResponseDto submitProgress(UUID userId, ProgressSubmitCommand cmd) {
        final int maxAttempts = 3;
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return txTemplate.execute(status -> performSubmitTx(userId, cmd));
            } catch (ConcurrencyFailureException ex) {
                if (attempt >= maxAttempts) throw ex;
                sleepBackoff(attempt);
            }
        }
    }

    // Extracted transaction body to reduce cognitive complexity and improve readability
    private ProgressSubmitResponseDto performSubmitTx(UUID userId, ProgressSubmitCommand cmd) {
        log.debug("submitProgress (tx) start: userId={}, progressId={}, selectedOptionIndex={}", userId, cmd.getProgressId(), cmd.getSelectedOptionIndex());

        if (cmd.getProgressId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "progressId is required");
        }

        // Lock user first to ensure consistent lock ordering (user -> progress) and avoid deadlocks
        User user = userRepository.findByIdForUpdate(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        // Now load and lock progress row
        Progress progress = progressRepository.findByIdForUpdate(cmd.getProgressId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Progress not found"));

        // Validate ownership
        if (progress.getUser() == null || !progress.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Progress does not belong to user");
        }

        if (progress.isFinalized()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Progress already submitted");
        }

        Task task = progress.getTask();
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Associated task not found");
        }

        boolean isCorrect = determineCorrectness(cmd.getSelectedOptionIndex(), task.getCorrectOptionIndex());
        int pointsAwarded = isCorrect ? 1 : 0;

        int levelsGained = updateUserStatsAndReturnLevelsGained(user, pointsAwarded);
        boolean levelUp = levelsGained > 0;
        // Clear active progress for the user since this progress is finalized
        user.setActiveProgressId(null);
        userRepository.save(user);

        // Mark task inactive so it won't be re-generated/used again
        Task taskToDeactivate = progress.getTask();
        if (taskToDeactivate != null) {
            taskToDeactivate.setActive(false);
            taskRepository.save(taskToDeactivate);
        }

        // Update and finalize progress
        progress.setSelectedOptionIndex(cmd.getSelectedOptionIndex());
        progress.setCorrect(isCorrect);
        progress.setPointsAwarded(pointsAwarded);
        progress.setTimeTakenMs(cmd.getTimeTakenMs());
        progress.setLevelUp(levelUp);
        progress.setFinalized(true);
        Progress savedProgress = progressRepository.save(progress);

        // Prepare and return response (include actual stars awarded count)
        ProgressSubmitResponseDto response = buildResponse(savedProgress, user, levelsGained);
        log.info("submitProgress completed: userId={}, progressId={}, isCorrect={}", userId, savedProgress.getId(), isCorrect);
        return response;
    }

    private void sleepBackoff(int attempt) {
        try { Thread.sleep(50L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    private boolean determineCorrectness(Short selectedOptionIndex, short correctIndex) {
        return selectedOptionIndex != null && selectedOptionIndex == correctIndex;
    }

    /**
     * Update user's points/level/stars and return how many levels (stars) were gained by this submission.
     */
    private int updateUserStatsAndReturnLevelsGained(User user, int pointsAwarded) {
        int newUserPoints = user.getPoints() + pointsAwarded;
        int previousThresholdCount = user.getPoints() / 50;
        int newThresholdCount = newUserPoints / 50;
        int levelsGained = Math.max(0, newThresholdCount - previousThresholdCount);

        user.setPoints(newUserPoints);
        user.setStars(user.getStars() + levelsGained);
        user.setCurrentLevel((short) (user.getCurrentLevel() + levelsGained));
        return levelsGained;
    }

    private ProgressSubmitResponseDto buildResponse(Progress saved, User user, int starsAwarded) {
        String explanation = saved.getTask() != null ? saved.getTask().getExplanation() : null;
        return new ProgressSubmitResponseDto(saved.getId(), saved.isCorrect(), saved.getPointsAwarded(), user.getPoints(), starsAwarded, saved.isLevelUp(), user.getCurrentLevel(), explanation);
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
