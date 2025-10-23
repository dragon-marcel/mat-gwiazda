package pl.matgwiazda.mapper;

import org.mapstruct.Mapper;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.domain.enums.UserRole;
import pl.matgwiazda.dto.UserDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    // MapStruct will pick this up when converting UserRole -> String
    @SuppressWarnings("unused")
    default String mapRoleToString(UserRole role) {
        return role != null ? role.name() : null;
    }
}
