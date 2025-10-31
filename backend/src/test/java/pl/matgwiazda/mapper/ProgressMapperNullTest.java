package pl.matgwiazda.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import pl.matgwiazda.domain.entity.Progress;
import pl.matgwiazda.dto.ProgressDto;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProgressMapperNullTest {

    private final ProgressMapper mapper = Mappers.getMapper(ProgressMapper.class);

    @Test
    void toDto_handlesNullRelations() {
        Progress p = new Progress();
        UUID id = UUID.randomUUID();
        p.setId(id);
        p.setUser(null);
        p.setTask(null);
        p.setAttemptNumber(1);
        p.setSelectedOptionIndex(null);
        p.setCorrect(false);
        p.setPointsAwarded(0);
        p.setTimeTakenMs(null);
        p.setFinalized(false);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());

        ProgressDto dto = mapper.toDto(p);
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertNull(dto.getUserId());
        assertNull(dto.getTaskId());
        assertEquals(1, dto.getAttemptNumber());
        assertNull(dto.getSelectedOptionIndex());
        assertFalse(dto.isCorrect());
        assertEquals(0, dto.getPointsAwarded());
        assertNull(dto.getTimeTakenMs());
    }
}
