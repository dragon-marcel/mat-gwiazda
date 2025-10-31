package pl.matgwiazda.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.domain.enums.UserRole;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.dto.AuthRegisterCommand;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    @SuppressWarnings("unused")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activeProgressId", ignore = true)
    User toEntity(UserDto dto);

    @SuppressWarnings("unused")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activeProgressId", ignore = true)
    User fromRegister(AuthRegisterCommand cmd);

    // Map role enum to String for DTO
    @SuppressWarnings("unused")
    default String mapRoleToString(UserRole role) {
        return role != null ? role.name() : null;
    }

    // Map role String back to UserRole enum when mapping DTO -> entity
    @SuppressWarnings("unused")
    default UserRole mapStringToRole(String role) {
        if (role == null) return null;
        try {
            return UserRole.valueOf(role);
        } catch (IllegalArgumentException ex) {
            return null; // unknown role handled as null; service should validate
        }
    }
}
