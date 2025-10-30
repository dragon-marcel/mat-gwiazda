package pl.matgwiazda.integration;

// Integration tests for AuthService

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.matgwiazda.dto.AuthLoginCommand;
import pl.matgwiazda.dto.AuthRegisterCommand;
import pl.matgwiazda.dto.AuthRefreshCommand;
import pl.matgwiazda.dto.AuthResponseDto;
import pl.matgwiazda.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class AuthServiceIntegrationTest extends IntegrationTestBase {


    @Autowired
    private AuthService authService;

    @Test
    void registerAndLogin_shouldReturnTokens() {
        AuthRegisterCommand reg = new AuthRegisterCommand("inttest@example.com", "strongpass", "inttest");
        AuthResponseDto resp = authService.register(reg);
        assertThat(resp).isNotNull();
        assertThat(resp.getAccessToken()).isNotBlank();
        assertThat(resp.getRefreshToken()).isNotBlank();
        assertThat(resp.getExpiresIn()).isGreaterThan(0L);

        AuthLoginCommand login = new AuthLoginCommand();
        login.setEmail("inttest@example.com");
        login.setPassword("strongpass");
        AuthResponseDto loginResp = authService.login(login);
        assertThat(loginResp).isNotNull();
        assertThat(loginResp.getAccessToken()).isNotBlank();
    }

    @Test
    void refreshToken_shouldReturnNewTokens() {
        // register to obtain refresh token
        AuthRegisterCommand reg = new AuthRegisterCommand("refreshint@example.com", "refreshpass", "refreshuser");
        AuthResponseDto resp = authService.register(reg);
        assertThat(resp).isNotNull();
        String refreshToken = resp.getRefreshToken();
        assertThat(refreshToken).isNotBlank();

        // refresh
        AuthRefreshCommand cmd = new AuthRefreshCommand(refreshToken);
        AuthResponseDto refreshed = authService.refresh(cmd);
        assertThat(refreshed).isNotNull();
        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotBlank();
        assertThat(refreshed.getExpiresIn()).isGreaterThan(0L);
    }

    @Test
    void loginWithWrongPassword_shouldThrowUnauthorized() {
        AuthRegisterCommand reg = new AuthRegisterCommand("badlogin@example.com", "correctpass", "baduser");
        authService.register(reg);

        AuthLoginCommand login = new AuthLoginCommand();
        login.setEmail("badlogin@example.com");
        login.setPassword("wrongpass");

        assertThatThrownBy(() -> authService.login(login))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
