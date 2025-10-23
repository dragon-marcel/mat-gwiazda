package pl.matgwiazda.mapper;

import org.mapstruct.Mapper;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.dto.TaskDto;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    // Map entity to DTO. MapStruct will map List<String> options automatically now.
    TaskDto toDto(Task task);

}
