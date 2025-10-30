package pl.matgwiazda.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.service.AdminService;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void listUsersShouldReturnList() {
        UUID id = UUID.randomUUID();
        UserDto u = new UserDto(id, "a@b.com", "a", "ADMIN", true);
        List<UserDto> list = List.of(u);

        when(adminService.listAllUsers()).thenReturn(list);

        var response = adminController.listUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(list, response.getBody());
        verify(adminService).listAllUsers();
    }
}
