// ...existing code...
package pl.matgwiazda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.matgwiazda.domain.entity.LearningLevel;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.TaskGenerateCommand;
import pl.matgwiazda.dto.TaskWithProgressDto;
import pl.matgwiazda.repository.LearningLevelRepository;
import pl.matgwiazda.repository.ProgressRepository;
import pl.matgwiazda.repository.TaskRepository;
import pl.matgwiazda.repository.UserRepository;
import pl.matgwiazda.service.AiTaskResult;
import pl.matgwiazda.service.OpenRouterService;
import pl.matgwiazda.service.TaskService;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class TaskServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LearningLevelRepository learningLevelRepository;

    @MockitoBean
    private OpenRouterService openRouterService;

    @Test
    void generateTask_withLearningLevel_createsTaskAndProgress() {
        // prepare learning level
        LearningLevel lvl = new LearningLevel();
        lvl.setLevel((short)5);
        lvl.setTitle("L5");
        lvl.setDescription("seed description");
        learningLevelRepository.save(lvl);

        // prepare user
        User u = new User();
        u.setEmail("taskint@example.com");
        u.setPassword("p");
        u.setUserName("taskuser");
        User savedUser = userRepository.save(u);

        // mock AI result
        AiTaskResult ai = new AiTaskResult("Solve 2+2", Arrays.asList("3","4","5"), 1, "Because 2+2=4");
        when(openRouterService.generateTaskFromSeed(anyString())).thenReturn(ai);

        TaskGenerateCommand cmd = new TaskGenerateCommand();
        cmd.setLevel((short)5);
        cmd.setCreatedById(null);

        TaskWithProgressDto result = taskService.generateTask(cmd, savedUser.getId());

        assertThat(result).isNotNull();
        assertThat(result.getTask()).isNotNull();
        assertThat(result.getTask().getPrompt()).contains("2+2");
        assertThat(result.getProgressId()).isNotNull();

        // ensure persisted
        assertThat(taskRepository.count()).isGreaterThan(0);
        assertThat(progressRepository.count()).isGreaterThan(0);
    }
}
// ...existing code...
