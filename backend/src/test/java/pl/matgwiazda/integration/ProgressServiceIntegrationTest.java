package pl.matgwiazda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.matgwiazda.domain.entity.Progress;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.ProgressSubmitCommand;
import pl.matgwiazda.dto.ProgressSubmitResponseDto;
import pl.matgwiazda.repository.ProgressRepository;
import pl.matgwiazda.repository.TaskRepository;
import pl.matgwiazda.repository.UserRepository;
import pl.matgwiazda.service.ProgressService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ProgressServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ProgressService progressService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Test
    void submitProgress_correctAnswer_awardsPointAndFinalizes() {
        // create user
        User u = new User();
        u.setEmail("puser@example.com");
        u.setPassword("p");
        u.setUserName("puser");
        User savedUser = userRepository.save(u);

        // create task with correct index 1
        Task t = new Task();
        t.setLevel((short)1);
        t.setPrompt("What is 2+2?");
        t.setOptions(java.util.Arrays.asList("3","4","5"));
        t.setCorrectOptionIndex((short)1);
        t.setExplanation("Because 2+2=4");
        Task savedTask = taskRepository.save(t);

        // create progress
        Progress p = new Progress();
        p.setUser(savedUser);
        p.setTask(savedTask);
        p.setAttemptNumber(0);
        Progress savedProgress = progressRepository.save(p);

        // set user's active progress
        savedUser.setActiveProgressId(savedProgress.getId());
        userRepository.save(savedUser);

        ProgressSubmitCommand cmd = new ProgressSubmitCommand();
        cmd.setProgressId(savedProgress.getId());
        cmd.setSelectedOptionIndex((short)1);
        cmd.setTimeTakenMs(1234);

        ProgressSubmitResponseDto resp = progressService.submitProgress(savedUser.getId(), cmd);

        assertThat(resp).isNotNull();
        assertThat(resp.isCorrect()).isTrue();
        assertThat(resp.getPointsAwarded()).isGreaterThanOrEqualTo(1);

        // verify DB updates
        Progress fromDb = progressRepository.findById(savedProgress.getId()).orElseThrow();
        assertThat(fromDb.isFinalized()).isTrue();
        Task taskFromDb = taskRepository.findById(savedTask.getId()).orElseThrow();
        assertThat(taskFromDb.isActive()).isFalse();
        User userFromDb = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(userFromDb.getActiveProgressId()).isNull();
    }
}

