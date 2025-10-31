package pl.matgwiazda.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.UserDto;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperToEntityTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toEntity_mapsFields_and_convertsRoleString() {
        UserDto dto = new UserDto();
        UUID id = UUID.randomUUID();
        dto.setId(id);
        dto.setEmail("alice@example.com");
        dto.setUserName("alice");
        dto.setRole("STUDENT");
        dto.setCurrentLevel((short)4);
        dto.setPoints(100);
        dto.setStars(2);
        dto.setActive(true);
        Instant now = Instant.now();
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);

        User entity = mapper.toEntity(dto);
        assertNotNull(entity);
        // id should be ignored by mapping
        assertNull(entity.getId());
        assertEquals("alice@example.com", entity.getEmail());
        assertEquals("alice", entity.getUserName());
        // role string should be converted to enum by mapper default method
        assertNotNull(entity.getRole());
        assertEquals("STUDENT", entity.getRole().name());
        assertEquals((short)4, entity.getCurrentLevel());
        assertEquals(100, entity.getPoints());
        assertEquals(2, entity.getStars());
        // password & activeProgressId are ignored
        assertNull(entity.getPassword());
        assertNull(entity.getActiveProgressId());
    }

    @Test
    void mapStringToRole_returnsNullForUnknownValue() {
        String bad = "UNKNOWN_ROLE";
        assertNull(mapper.mapStringToRole(bad));
        assertNull(mapper.mapStringToRole(null));
    }
}
