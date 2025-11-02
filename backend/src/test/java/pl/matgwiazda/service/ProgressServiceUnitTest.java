package pl.matgwiazda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matgwiazda.domain.entity.Progress;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.ProgressSubmitCommand;
import pl.matgwiazda.mapper.ProgressMapper;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceUnitTest {

    @Test
    void listAllProgress_nullUser_throws() {
        var progressRepo = mock(pl.matgwiazda.repository.ProgressRepository.class);
        var userRepo = mock(pl.matgwiazda.repository.UserRepository.class);
        var progressMapper = mock(ProgressMapper.class);
        var taskRepo = mock(pl.matgwiazda.repository.TaskRepository.class);

        ProgressService svc = new ProgressService(progressRepo, userRepo, progressMapper, taskRepo);

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> svc.listAllProgress(null));
    }

    @Test
    void submitProgress_correctAnswer_updatesUserPointsAndReturnsResponse() {
        var progressRepo = mock(pl.matgwiazda.repository.ProgressRepository.class);
        var userRepo = mock(pl.matgwiazda.repository.UserRepository.class);
        var progressMapper = mock(ProgressMapper.class);
        var taskRepo = mock(pl.matgwiazda.repository.TaskRepository.class);

        ProgressService svc = new ProgressService(progressRepo, userRepo, progressMapper, taskRepo);

        // Arrange existing progress with task and user
        UUID progId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setPoints(40);
        user.setStars(0);
        user.setCurrentLevel((short)1);

        Task task = new Task();
        task.setCorrectOptionIndex((short)1);
        task.setExplanation("explanation test........");

        Progress p = new Progress();
        p.setId(progId);
        p.setUser(user);
        p.setTask(task);
        p.setFinalized(false);

        when(progressRepo.findByIdForUpdate(progId)).thenReturn(Optional.of(p));
        when(progressRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProgressSubmitCommand cmd = new ProgressSubmitCommand();
        cmd.setProgressId(progId);
        cmd.setSelectedOptionIndex((short)1);
        cmd.setTimeTakenMs(123); // use Integer to match DTO signature

        var resp = svc.submitProgress(userId, cmd);

        // Assert
        assertNotNull(resp);
        assertTrue(resp.isCorrect());
        assertEquals(10, resp.getPointsAwarded());
        assertEquals(50, resp.getUserPoints());
        assertEquals(user.getStars(), resp.getStarsAwarded());
        assertFalse(resp.isLeveledUp());
        assertEquals(user.getCurrentLevel(), resp.getNewLevel());
        assertEquals("explanation test........", resp.getExplanation());

        verify(progressRepo).findByIdForUpdate(progId);
        verify(progressRepo).save(any());
        verify(userRepo).save(any());
    }

}
