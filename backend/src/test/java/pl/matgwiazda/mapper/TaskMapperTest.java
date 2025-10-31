package pl.matgwiazda.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.TaskDto;
import pl.matgwiazda.dto.openrouter.AiTaskResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskMapperTest {

    private final TaskMapper mapper = Mappers.getMapper(TaskMapper.class);

    @Test
    void toDto_mapsFields() {
        User u = new User();
        UUID userId = UUID.randomUUID();
        u.setId(userId);

        Task t = new Task();
        UUID taskId = UUID.randomUUID();
        t.setId(taskId);
        t.setLevel((short) 3);
        t.setPrompt("sample prompt");
        t.setOptions(List.of("opt1", "opt2"));
        t.setCorrectOptionIndex((short) 1);
        t.setExplanation("explanation text");
        t.setCreatedBy(u);
        t.setActive(true);
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());

        TaskDto dto = mapper.toDto(t);

        assertNotNull(dto);
        assertEquals(taskId, dto.getId());
        assertEquals(t.getLevel(), dto.getLevel());
        assertEquals(t.getPrompt(), dto.getPrompt());
        assertEquals(t.getOptions(), dto.getOptions());
        assertEquals(Integer.valueOf(t.getCorrectOptionIndex()), dto.getCorrectOptionIndex());
        assertEquals(userId, dto.getCreatedById());
        assertEquals(t.getExplanation(), dto.getExplanation());
    }

    @Test
    void fromAiResult_mapsCorrectIndexAndFields() {
        AiTaskResult ai = new AiTaskResult("ai prompt", List.of("a", "b", "c"), 2, "ai explanation");
        Task t = mapper.fromAiResult(ai);
        assertNotNull(t);
        assertEquals("ai prompt", t.getPrompt());
        assertEquals(ai.options(), t.getOptions());
        assertEquals((short) ai.correctIndex(), t.getCorrectOptionIndex());
        assertEquals("ai explanation", t.getExplanation());
    }

    @Test
    void toDto_handlesNullCreatedByAndZeroCorrectIndex() {
        Task t = new Task();
        UUID taskId = UUID.randomUUID();
        t.setId(taskId);
        t.setLevel((short)1);
        t.setPrompt("prompt null createdBy");
        t.setOptions(List.of("x","y"));
        t.setCorrectOptionIndex((short)0);
        t.setExplanation(null);
        t.setCreatedBy(null);

        TaskDto dto = mapper.toDto(t);

        assertNotNull(dto);
        assertEquals(taskId, dto.getId());
        assertNull(dto.getCreatedById());
        // MapStruct maps short 0 to Integer 0
        assertEquals(Integer.valueOf(0), dto.getCorrectOptionIndex());
        assertEquals(t.getOptions(), dto.getOptions());
    }
}
