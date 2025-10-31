package pl.matgwiazda.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import pl.matgwiazda.domain.enums.UserRole;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperRoleMappingTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void mapRoleToString_returnsNameForNonNull() {
        String s = mapper.mapRoleToString(UserRole.ADMIN);
        assertEquals("ADMIN", s);
    }

    @Test
    void mapRoleToString_returnsNullForNull() {
        String s = mapper.mapRoleToString(null);
        assertNull(s);
    }
}

