package pl.matgwiazda.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.matgwiazda.domain.entity.Progress;
import pl.matgwiazda.dto.ProgressDto;

@Mapper(componentModel = "spring")
public interface ProgressMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "task.id", target = "taskId")
    ProgressDto toDto(Progress p);
}

