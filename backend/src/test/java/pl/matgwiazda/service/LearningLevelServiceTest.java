package pl.matgwiazda.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.domain.entity.LearningLevel;
import pl.matgwiazda.dto.CreateLearningLevelCommand;
import pl.matgwiazda.dto.UpdateLearningLevelCommand;
import pl.matgwiazda.repository.LearningLevelRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningLevelServiceTest {

    @Mock
    LearningLevelRepository repository;

    @InjectMocks
    LearningLevelService service;

    LearningLevel sample;

    @BeforeEach
    void setUp() {
        sample = new LearningLevel();
        sample.setLevel((short)1);
        sample.setTitle("Poziom 1");
        sample.setDescription("Opis");
        sample.setCreatedAt(Instant.now());
        sample.setCreatedBy(UUID.randomUUID());
    }

    @Test
    void listAll_returnsDtos() {
        when(repository.findAll()).thenReturn(List.of(sample));

        var list = service.listAll();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).level()).isEqualTo(sample.getLevel());
        verify(repository).findAll();
    }

    @Test
    void getByLevel_found() {
        when(repository.findById((short)1)).thenReturn(Optional.of(sample));

        var dto = service.getByLevel((short)1);

        assertThat(dto.level()).isEqualTo((short)1);
    }

    @Test
    void getByLevel_notFound_throws() {
        when(repository.findById((short)2)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.getByLevel((short)2));
    }

    @Test
    void create_conflict_throws() {
        CreateLearningLevelCommand cmd = new CreateLearningLevelCommand((short)1, "T", "D");
        when(repository.existsById((short)1)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.create(cmd, UUID.randomUUID()));
        verify(repository, never()).save(any());
    }

    @Test
    void create_savesEntity_and_returnsDto() {
        CreateLearningLevelCommand cmd = new CreateLearningLevelCommand((short)2, " Title ", " Desc ");
        when(repository.existsById((short)2)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.create(cmd, UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThat(dto.level()).isEqualTo((short)2);
        assertThat(dto.title()).isEqualTo("Title");
        assertThat(dto.description()).isEqualTo("Desc");
        ArgumentCaptor<LearningLevel> cap = ArgumentCaptor.forClass(LearningLevel.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getCreatedBy()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void update_notFound_throws() {
        when(repository.findById((short)5)).thenReturn(Optional.empty());
        UpdateLearningLevelCommand cmd = new UpdateLearningLevelCommand(null, null, null);
        assertThrows(ResponseStatusException.class, () -> service.update((short)5, cmd, UUID.randomUUID()));
    }

    @Test
    void update_changes_and_saves() {
        when(repository.findById((short)1)).thenReturn(Optional.of(sample));
        UpdateLearningLevelCommand cmd = new UpdateLearningLevelCommand(null, " New ", " NewDesc ");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.update((short)1, cmd, UUID.fromString("00000000-0000-0000-0000-000000000002"));

        assertThat(dto.title()).isEqualTo("New");
        assertThat(dto.description()).isEqualTo("NewDesc");
        verify(repository).save(any());
    }

    @Test
    void delete_notFound_throws() {
        when(repository.existsById((short)9)).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> service.delete((short)9));
    }

    @Test
    void delete_exists_invokesDelete() {
        when(repository.existsById((short)1)).thenReturn(true);
        service.delete((short)1);
        verify(repository).deleteById((short)1);
    }
}
