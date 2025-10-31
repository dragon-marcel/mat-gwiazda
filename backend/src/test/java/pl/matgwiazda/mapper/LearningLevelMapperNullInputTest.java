package pl.matgwiazda.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import pl.matgwiazda.domain.entity.LearningLevel;
import pl.matgwiazda.dto.LearningLevelDto;

import static org.junit.jupiter.api.Assertions.*;

public class LearningLevelMapperNullInputTest {

    private final LearningLevelMapper mapper = Mappers.getMapper(LearningLevelMapper.class);

    @Test
    public void toDto_returnsNullForNullInput() {
        LearningLevelDto dto = mapper.toDto(null);
        assertNull(dto);
    }
}

