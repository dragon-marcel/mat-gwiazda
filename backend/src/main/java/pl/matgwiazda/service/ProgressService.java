package pl.matgwiazda.service;

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
import pl.matgwiazda.mapper.ProgressMapper;
import pl.matgwiazda.repository.ProgressRepository;
import pl.matgwiazda.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final ProgressMapper progressMapper;

    public ProgressService(ProgressRepository progressRepository, UserRepository userRepository, ProgressMapper progressMapper) {
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.progressMapper = progressMapper;
    }

    /**
     * Create a new initial Progress entity for given user (may be null) and task.
     */
    public Progress createInitialProgress(User user, Task savedTask) {
        Progress progress = new Progress();
        if (user != null) progress.setUser(user);
        progress.setTask(savedTask);
        progress.setAttemptNumber(1);
        progress.setCorrect(false);
        progress.setPointsAwarded(0);
        progress.setFinalized(false);
        return progress;
    }

    /**
     * Persist progress and, if user present, set user's activeProgressId and persist user.
     */
    public Progress persistProgressAndUpdateUser(Progress progress, User user) {
        Progress savedProgress = progressRepository.save(progress);
        if (user != null) {
            user.setActiveProgressId(savedProgress.getId());
            userRepository.save(user);
        }
        return savedProgress;
    }

    /**
     * Submit an answer for a previously created Progress record.
     * This method locks the progress row for update, validates ownership, marks it finalized and
     * updates user points. It returns a minimal response DTO used by the controller.
     */
    @Transactional
    public ProgressSubmitResponseDto submitProgress(UUID userId, ProgressSubmitCommand cmd) {
        if (cmd == null || cmd.getProgressId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "progressId is required");
        }

        Optional<Progress> opt = progressRepository.findByIdForUpdate(cmd.getProgressId());
        if (opt.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Progress not found");

        Progress p = opt.get();

        // If userId provided, ensure the progress belongs to that user
        if (userId != null && p.getUser() != null && !userId.equals(p.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Progress does not belong to the user");
        }

        if (p.isFinalized()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Progress already finalized");
        }

        Short selected = cmd.getSelectedOptionIndex();
        p.setSelectedOptionIndex(selected);
        p.setTimeTakenMs(cmd.getTimeTakenMs());

        // Determine correctness
        boolean correct = false;
        if (selected != null && p.getTask() != null) {
            short correctIdx = p.getTask().getCorrectOptionIndex();
            correct = (selected.shortValue() == correctIdx);
        }
        p.setCorrect(correct);
        p.setFinalized(true);

        // Simple points awarding policy: 10 points for correct, 0 otherwise
        int points = correct ? 10 : 0;
        p.setPointsAwarded(points);

        Progress saved = progressRepository.save(p);

        // Update user points if present
        int userPoints = 0;
        int starsAwarded = 0;
        boolean leveledUp = false;
        short newLevel = 0;
        if (saved.getUser() != null) {
            User u = saved.getUser();
            int newPoints = u.getPoints() + saved.getPointsAwarded();
            u.setPoints(newPoints);
            // leave stars/level logic simple for now (no level up)
            userRepository.save(u);
            userPoints = u.getPoints();
            starsAwarded = u.getStars();
            leveledUp = false;
            newLevel = u.getCurrentLevel();
        }

        String explanation = null;
        if (saved.getTask() != null) explanation = saved.getTask().getExplanation();

        return new ProgressSubmitResponseDto(saved.getId(), saved.isCorrect(), saved.getPointsAwarded(), userPoints, starsAwarded, leveledUp, newLevel, explanation);
    }

    /**
     * List all progress entries for a user (non-paged). Returns DTOs mapped with ProgressMapper.
     */
    public List<ProgressDto> listAllProgress(UUID userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        List<Progress> list = progressRepository.findByUserId(userId, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ProgressDto> out = new ArrayList<>();
        for (Progress p : list) {
            out.add(progressMapper.toDto(p));
        }
        return out;
    }
}
