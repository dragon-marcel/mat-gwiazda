package pl.matgwiazda.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.AuthLoginCommand;
import pl.matgwiazda.dto.AuthRegisterCommand;
import pl.matgwiazda.dto.AuthRefreshCommand;
import pl.matgwiazda.dto.AuthResponseDto;
import pl.matgwiazda.repository.UserRepository;
import pl.matgwiazda.security.JwtService;
import pl.matgwiazda.mapper.UserMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;
    @Mock
    UserMapper userMapper;

    @InjectMocks
    AuthService authService;

    @Test
    void register_success_createsUser_and_returnsTokens() {
        AuthRegisterCommand cmd = new AuthRegisterCommand("test@example.com", "pass", "User");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("hashed");
        when(jwtService.generateAccessToken(any())).thenReturn("acc");
        when(jwtService.generateRefreshToken(any())).thenReturn("ref");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(3600000L);

        // stub mapper to produce a User instance (mapper normally maps DTO to entity)
        when(userMapper.fromRegister(any(AuthRegisterCommand.class))).thenAnswer(inv -> {
            AuthRegisterCommand c = inv.getArgument(0);
            User u = new User();
            u.setEmail(c.getEmail());
            u.setUserName(c.getUserName());
            return u;
        });

        AuthResponseDto resp = authService.register(cmd);

        assertThat(resp.getAccessToken()).isEqualTo("acc");
        assertThat(resp.getRefreshToken()).isEqualTo("ref");
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().getEmail()).isEqualTo("test@example.com");
        assertThat(cap.getValue().getPassword()).isEqualTo("hashed");
    }

    @Test
    void register_existingEmail_throws() {
        AuthRegisterCommand cmd = new AuthRegisterCommand("a@b.com", "p", "u");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(new User()));
        assertThrows(ResponseStatusException.class, () -> authService.register(cmd));
    }

    @Test
    void login_success_returnsTokens() {
        AuthLoginCommand cmd = new AuthLoginCommand();
        cmd.setEmail("x@y.com");
        cmd.setPassword("pw");
        User u = new User();
        u.setPassword("hash");
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(u)).thenReturn("a");
        when(jwtService.generateRefreshToken(u)).thenReturn("r");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(60000L);

        AuthResponseDto resp = authService.login(cmd);

        assertThat(resp.getAccessToken()).isEqualTo("a");
    }

    @Test
    void login_invalidUser_throws() {
        AuthLoginCommand cmd = new AuthLoginCommand();
        cmd.setEmail("no@one.com");
        when(userRepository.findByEmail("no@one.com")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> authService.login(cmd));
    }

    @Test
    void login_wrongPassword_throws() {
        AuthLoginCommand cmd = new AuthLoginCommand();
        cmd.setEmail("u@u.com");
        cmd.setPassword("pw");
        User u = new User(); u.setPassword("h");
        when(userRepository.findByEmail("u@u.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "h")).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> authService.login(cmd));
    }

    @Test
    void refresh_success_returnsNewTokens() {
        AuthRefreshCommand cmd = new AuthRefreshCommand();
        cmd.setRefreshToken("rt");
        Claims claims = mock(Claims.class);
        when(claims.get("type")).thenReturn("refresh");
        when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(jwtService.parseClaims("rt")).thenReturn(claims);
        UUID id = UUID.randomUUID();
        when(claims.getSubject()).thenReturn(id.toString());
        User u = new User(); u.setId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(jwtService.generateAccessToken(u)).thenReturn("a");
        when(jwtService.generateRefreshToken(u)).thenReturn("r");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(1000L);

        AuthResponseDto resp = authService.refresh(cmd);
        assertThat(resp.getAccessToken()).isEqualTo("a");
    }

    @Test
    void refresh_wrongType_throws() {
        AuthRefreshCommand cmd = new AuthRefreshCommand();
        cmd.setRefreshToken("t");
        Claims claims = mock(Claims.class);
        when(claims.get("type")).thenReturn("access");
        when(jwtService.parseClaims("t")).thenReturn(claims);
        assertThrows(ResponseStatusException.class, () -> authService.refresh(cmd));
    }
}
