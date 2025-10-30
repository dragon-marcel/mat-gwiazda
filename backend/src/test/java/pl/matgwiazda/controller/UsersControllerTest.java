package pl.matgwiazda.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.dto.UserUpdateCommand;
import pl.matgwiazda.service.UserService;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UsersController usersController;

    @Test
    void getMeShouldReturnUserDto() {
        User user = new User();
        UUID id = UUID.randomUUID();
        user.setId(id);
        user.setEmail("u@example.com");
        user.setUserName("u");

        UserDto dto = new UserDto(id, "u@example.com", "u", "STUDENT", true);
        when(userService.getUserDtoFromEntity(user)).thenReturn(dto);

        var response = usersController.getMe(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
        verify(userService).getUserDtoFromEntity(user);
    }

    @Test
    void updateMeShouldCallServiceAndReturnDto() {
        User user = new User();
        UUID id = UUID.randomUUID();
        user.setId(id);

        UserUpdateCommand cmd = new UserUpdateCommand();
        cmd.setUserName("newname");

        UserDto dto = new UserDto(id, "u@example.com", "newname", "STUDENT", true);
        when(userService.updateUser(id, cmd)).thenReturn(dto);

        var response = usersController.updateMe(user, cmd);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
        verify(userService).updateUser(id, cmd);
    }

    @Test
    void deleteMeShouldDeactivateAndReturnNoContent() {
        User user = new User();
        UUID id = UUID.randomUUID();
        user.setId(id);

        var response = usersController.deleteMe(user);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).deactivateUser(id);
    }
}

