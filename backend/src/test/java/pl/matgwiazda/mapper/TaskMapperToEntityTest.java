package pl.matgwiazda.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.dto.TaskDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskMapperToEntityTest {

    private final TaskMapper mapper = Mappers.getMapper(TaskMapper.class);

    @Test
    void toEntity_mapsFields_and_ignoresAuditFields() {
        TaskDto dto = new TaskDto();
        UUID id = UUID.randomUUID();
        dto.setId(id);
        dto.setLevel((short)2);
        dto.setPrompt("dto prompt");
        dto.setOptions(List.of("o1","o2"));
        dto.setCorrectOptionIndex(1);
        dto.setExplanation("explanation dto");
        dto.setCreatedById(UUID.randomUUID());
        dto.setActive(true);
        dto.setCreatedAt(Instant.now());
        dto.setUpdatedAt(Instant.now());

        Task ent = mapper.toEntity(dto);
        assertNotNull(ent);
        // id and createdBy should be ignored by mapping
        assertNull(ent.getId());
        assertNull(ent.getCreatedBy());
        // fields should be mapped
        assertEquals(dto.getPrompt(), ent.getPrompt());
        assertEquals(dto.getOptions(), ent.getOptions());
        assertEquals((short) dto.getCorrectOptionIndex().intValue(), ent.getCorrectOptionIndex());
        assertEquals(dto.getExplanation(), ent.getExplanation());
    }
}
