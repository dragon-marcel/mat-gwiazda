package pl.matgwiazda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import pl.matgwiazda.dto.AuthLoginCommand;
import pl.matgwiazda.dto.AuthRefreshCommand;
import pl.matgwiazda.dto.AuthRegisterCommand;
import pl.matgwiazda.dto.AuthResponseDto;
import pl.matgwiazda.repository.UserRepository;
import pl.matgwiazda.security.JwtService;
import pl.matgwiazda.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void registerShouldReturnCreated() throws Exception {
        AuthRegisterCommand cmd = new AuthRegisterCommand("a@b.com", "secret123", "tester");
        AuthResponseDto resp = new AuthResponseDto();
        resp.setAccessToken("acc-token");
        resp.setRefreshToken("ref-token");

        when(authService.register(any(AuthRegisterCommand.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("acc-token"))
                .andExpect(jsonPath("$.refreshToken").value("ref-token"));

        verify(authService).register(any(AuthRegisterCommand.class));
    }

    @Test
    void loginShouldReturnOk() throws Exception {
        AuthLoginCommand cmd = new AuthLoginCommand();
        cmd.setEmail("x@y.com");
        cmd.setPassword("pwd");

        AuthResponseDto resp = new AuthResponseDto();
        resp.setAccessToken("acc");
        resp.setRefreshToken("ref");

        when(authService.login(any(AuthLoginCommand.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acc"));

        verify(authService).login(any(AuthLoginCommand.class));
    }

    @Test
    void refreshShouldReturnOk() throws Exception {
        AuthRefreshCommand cmd = new AuthRefreshCommand();
        cmd.setRefreshToken("ref-token");

        AuthResponseDto resp = new AuthResponseDto();
        resp.setAccessToken("new-acc");
        resp.setRefreshToken("new-ref");

        when(authService.refresh(any(AuthRefreshCommand.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-acc"));

        verify(authService).refresh(any(AuthRefreshCommand.class));
    }
}
