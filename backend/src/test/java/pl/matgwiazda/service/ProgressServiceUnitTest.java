package pl.matgwiazda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.ProgressSubmitCommand;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ProgressServiceUnitTest {

    @Test
    void updateUserStats_levelsGained_and_userUpdated() throws Exception {
        // Arrange
        var progressRepo = mock(pl.matgwiazda.repository.ProgressRepository.class);
        var userRepo = mock(pl.matgwiazda.repository.UserRepository.class);
        var taskRepo = mock(pl.matgwiazda.repository.TaskRepository.class);
        var txManager = mock(PlatformTransactionManager.class);

        ProgressService svc = new ProgressService(progressRepo, userRepo, taskRepo, txManager);

        User user = new User();
        user.setPoints(49);
        user.setStars(0);
        user.setCurrentLevel((short)1);

        // Use reflection to invoke private method updateUserStatsAndReturnLevelsGained
        Method m = ProgressService.class.getDeclaredMethod("updateUserStatsAndReturnLevelsGained", User.class, int.class);
        m.setAccessible(true);

        // Act
        int gained = (int) m.invoke(svc, user, 1);

        // Assert
        assertEquals(1, gained, "Should gain 1 level when reaching 50 points");
        assertEquals(50, user.getPoints(), "User points should be updated");
        assertEquals(1, user.getStars(), "User stars should be incremented");
        assertEquals(2, user.getCurrentLevel(), "User current level should be incremented by 1");
    }

    @Test
    void updateUserStats_noLevelGain_when_thresholdNotReached() throws Exception {
        var progressRepo = mock(pl.matgwiazda.repository.ProgressRepository.class);
        var userRepo = mock(pl.matgwiazda.repository.UserRepository.class);
        var taskRepo = mock(pl.matgwiazda.repository.TaskRepository.class);
        var txManager = mock(PlatformTransactionManager.class);

        ProgressService svc = new ProgressService(progressRepo, userRepo, taskRepo, txManager);

        User user = new User();
        user.setPoints(100);
        user.setStars(2);
        user.setCurrentLevel((short)3);

        Method m = ProgressService.class.getDeclaredMethod("updateUserStatsAndReturnLevelsGained", User.class, int.class);
        m.setAccessible(true);

        int gained = (int) m.invoke(svc, user, 1);

        assertEquals(0, gained, "Should not gain a level when not crossing 50-point boundary");
        assertEquals(101, user.getPoints());
        assertEquals(2, user.getStars());
        assertEquals(3, user.getCurrentLevel());
    }

    @Test
    void listAllProgress_nullUser_throws() {
        var progressRepo = mock(pl.matgwiazda.repository.ProgressRepository.class);
        var userRepo = mock(pl.matgwiazda.repository.UserRepository.class);
        var taskRepo = mock(pl.matgwiazda.repository.TaskRepository.class);
        var txManager = mock(PlatformTransactionManager.class);

        ProgressService svc = new ProgressService(progressRepo, userRepo, taskRepo, txManager);

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> svc.listAllProgress(null));
    }

    @Test
    void submitProgress_nullProgressId_throwsBadRequest() throws Exception {
        var progressRepo = mock(pl.matgwiazda.repository.ProgressRepository.class);
        var userRepo = mock(pl.matgwiazda.repository.UserRepository.class);
        var taskRepo = mock(pl.matgwiazda.repository.TaskRepository.class);
        var txManager = mock(PlatformTransactionManager.class);

        ProgressService svc = new ProgressService(progressRepo, userRepo, taskRepo, txManager);

        // Replace internal txTemplate with executor that directly invokes callback to avoid real transactions
        TransactionTemplate tt = new TransactionTemplate(null) {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                try {
                    return action.doInTransaction(null);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Field f = ProgressService.class.getDeclaredField("txTemplate");
        f.setAccessible(true);
        f.set(svc, tt);

        UUID userId = UUID.randomUUID();
        ProgressSubmitCommand cmd = new ProgressSubmitCommand();
        cmd.setProgressId(null); // invalid
        cmd.setSelectedOptionIndex((short)1);

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> svc.submitProgress(userId, cmd));
    }

    @Test
    void determineCorrectness_privateMethod_behaviour() throws Exception {
        var progressRepo = mock(pl.matgwiazda.repository.ProgressRepository.class);
        var userRepo = mock(pl.matgwiazda.repository.UserRepository.class);
        var taskRepo = mock(pl.matgwiazda.repository.TaskRepository.class);
        var txManager = mock(PlatformTransactionManager.class);

        ProgressService svc = new ProgressService(progressRepo, userRepo, taskRepo, txManager);

        Method m = ProgressService.class.getDeclaredMethod("determineCorrectness", Short.class, short.class);
        m.setAccessible(true);

        boolean correct = (boolean) m.invoke(svc, (short)2, (short)2);
        assertTrue(correct);
        boolean incorrect = (boolean) m.invoke(svc, (Short) null, (short)2);
        assertFalse(incorrect);
    }

}
