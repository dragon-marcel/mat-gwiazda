package pl.matgwiazda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.matgwiazda.dto.CreateLearningLevelCommand;
import pl.matgwiazda.dto.LearningLevelDto;
import pl.matgwiazda.dto.UpdateLearningLevelCommand;
import pl.matgwiazda.repository.LearningLevelRepository;
import pl.matgwiazda.service.LearningLevelService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class LearningLevelServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private LearningLevelService learningLevelService;

    @Autowired
    private LearningLevelRepository learningLevelRepository;

    @Test
    void create_get_update_and_delete_level() {
        CreateLearningLevelCommand create = new CreateLearningLevelCommand();
        create.setLevel((short)10);
        create.setTitle("L10");
        create.setDescription("desc");

        UUID actor = UUID.randomUUID();
        LearningLevelDto created = learningLevelService.create(create, actor);
        assertThat(created).isNotNull();
        assertThat(created.level()).isEqualTo((short)10);

        LearningLevelDto fetched = learningLevelService.getByLevel((short)10);
        assertThat(fetched.title()).isEqualTo("L10");

        UpdateLearningLevelCommand upd = new UpdateLearningLevelCommand();
        upd.setTitle("L10-upd");
        LearningLevelDto updated = learningLevelService.update((short)10, upd, actor);
        assertThat(updated.title()).isEqualTo("L10-upd");

        learningLevelService.delete((short)10);
        assertThat(learningLevelRepository.existsById((short)10)).isFalse();
    }
}

