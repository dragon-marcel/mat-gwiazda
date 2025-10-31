package pl.matgwiazda.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.dto.TaskDto;
import pl.matgwiazda.dto.openrouter.AiTaskResult;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    // Map entity to DTO. MapStruct will map List<String> options automatically.
    // Map createdBy.id -> createdById and convert primitive short -> Integer for correctOptionIndex
    @Mapping(source = "createdBy.id", target = "createdById")
    @Mapping(source = "correctOptionIndex", target = "correctOptionIndex", qualifiedByName = "shortToInteger")
    TaskDto toDto(Task task);

    // Map DTO -> Entity for admin operations. We ignore createdBy and audit fields so the service
    // layer can control associations and persistence-related fields (Single Responsibility).
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Task toEntity(TaskDto dto);

    // Map AiTaskResult -> Task entity. Convert correctIndex (int) -> correctOptionIndex (short).
    // We ignore id/createdBy/audit fields; service will populate them.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "correctIndex", target = "correctOptionIndex", qualifiedByName = "intToShort")
    Task fromAiResult(AiTaskResult ai);

    @Named("intToShort")
    default short intToShort(int value) {
        return (short) value;
    }

    @Named("shortToInteger")
    default Integer shortToInteger(short value) {
        return Integer.valueOf(value);
    }

}
