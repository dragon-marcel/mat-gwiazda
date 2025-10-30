package pl.matgwiazda.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matgwiazda.dto.AuthLoginCommand;
import pl.matgwiazda.dto.AuthRegisterCommand;
import pl.matgwiazda.dto.AuthResponseDto;
import pl.matgwiazda.dto.AuthRefreshCommand;
import pl.matgwiazda.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerShouldReturnCreatedResponse() {
        AuthRegisterCommand cmd = new AuthRegisterCommand();
        cmd.setEmail("test@example.com");
        cmd.setPassword("secret");
        cmd.setUserName("tester");

        AuthResponseDto resp = new AuthResponseDto();
        resp.setAccessToken("access");
        resp.setRefreshToken("refresh");

        when(authService.register(cmd)).thenReturn(resp);

        var response = authController.register(cmd);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(resp, response.getBody());
        verify(authService).register(cmd);
    }

    @Test
    void registerShouldThrowWhenBodyIsNull() {
        assertThrows(ResponseStatusException.class, () -> authController.register(null));
    }

    @Test
    void loginShouldReturnOkResponse() {
        AuthLoginCommand cmd = new AuthLoginCommand();
        cmd.setEmail("a@b.com");
        cmd.setPassword("p");

        AuthResponseDto resp = new AuthResponseDto();
        resp.setAccessToken("acc");
        when(authService.login(cmd)).thenReturn(resp);

        var response = authController.login(cmd);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(resp, response.getBody());
        verify(authService).login(cmd);
    }

    @Test
    void refreshShouldReturnOkResponse() {
        AuthRefreshCommand cmd = new AuthRefreshCommand();
        cmd.setRefreshToken("r");

        AuthResponseDto resp = new AuthResponseDto();
        resp.setAccessToken("acc");
        when(authService.refresh(cmd)).thenReturn(resp);

        var response = authController.refresh(cmd);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(resp, response.getBody());
        verify(authService).refresh(cmd);
    }
}

