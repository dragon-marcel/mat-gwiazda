package pl.matgwiazda.mapper;

import org.mapstruct.Mapper;
import pl.matgwiazda.domain.entity.LearningLevel;
import pl.matgwiazda.dto.LearningLevelDto;

@Mapper(componentModel = "spring")
public interface LearningLevelMapper {

    // MapStruct will map fields by name automatically (level, title, description, createdBy, createdAt, modifiedBy, modifiedAt)
    LearningLevelDto toDto(LearningLevel ent);
}

