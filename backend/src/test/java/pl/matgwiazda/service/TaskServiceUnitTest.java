package pl.matgwiazda.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matgwiazda.domain.entity.Progress;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.TaskDto;
import pl.matgwiazda.dto.TaskGenerateCommand;
import pl.matgwiazda.dto.openrouter.AiTaskResult;
import pl.matgwiazda.mapper.TaskMapper;
import pl.matgwiazda.repository.LearningLevelRepository;
import pl.matgwiazda.repository.ProgressRepository;
import pl.matgwiazda.repository.TaskRepository;
import pl.matgwiazda.repository.UserRepository;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceUnitTest {

    @Mock
    TaskRepository taskRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    TaskMapper taskMapper;
    @Mock
    ProgressRepository progressRepository;
    @Mock
    LearningLevelRepository learningLevelRepository;
    @Mock
    OpenRouterService openRouterService;
    @Mock
    PlatformTransactionManager txManager;

    TaskService svc;

    @BeforeEach
    void setUp() throws Exception {
        svc = new TaskService(taskRepository, userRepository, taskMapper, progressRepository, txManager, learningLevelRepository, openRouterService);
        // Replace internal TransactionTemplate with a no-op implementation that executes the callback directly
        TransactionTemplate tt = new TransactionTemplate(null) {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                try {
                    return action.doInTransaction(null);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Field f = TaskService.class.getDeclaredField("txTemplate");
        f.setAccessible(true);
        f.set(svc, tt);
    }

    @Test
    void generateTask_returnsExistingIfUserHasActiveProgress() {
        UUID userId = UUID.randomUUID();
        UUID progId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setActiveProgressId(progId);

        Task existingTask = new Task();
        existingTask.setId(UUID.randomUUID());

        Progress existingProgress = new Progress();
        existingProgress.setId(progId);
        existingProgress.setTask(existingTask);
        existingProgress.setUser(user);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(progressRepository.findById(progId)).thenReturn(Optional.of(existingProgress));
        when(taskMapper.toDto(existingTask)).thenReturn(new TaskDto());

        var result = svc.generateTask(new TaskGenerateCommand(), userId);

        assertThat(result.getProgressId()).isEqualTo(progId);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void generateTask_createsTaskAndProgress_whenAiProvidesResult() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        // Provide learning level present to trigger AI call
        when(learningLevelRepository.findById(any())).thenReturn(Optional.of(new pl.matgwiazda.domain.entity.LearningLevel()));
        when(openRouterService.generateTaskFromSeed(any())).thenReturn(new AiTaskResult("p", java.util.List.of("a","b","c","d"), 1, "exp"));

        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(progressRepository.save(any())).thenAnswer(inv -> {
            Progress p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(taskMapper.toDto(any())).thenReturn(new TaskDto());

        var cmd = new TaskGenerateCommand();
        cmd.setLevel((short)1);

        var res = svc.generateTask(cmd, userId);

        assertThat(res.getTask()).isNotNull();
        verify(taskRepository).save(any());
        verify(progressRepository).save(any());
        verify(userRepository).save(any());
    }
}
