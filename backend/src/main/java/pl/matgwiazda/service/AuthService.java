package pl.matgwiazda.service;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.domain.enums.UserRole;
import pl.matgwiazda.dto.AuthLoginCommand;
import pl.matgwiazda.dto.AuthRefreshCommand;
import pl.matgwiazda.dto.AuthRegisterCommand;
import pl.matgwiazda.dto.AuthResponseDto;
import pl.matgwiazda.repository.UserRepository;
import pl.matgwiazda.security.JwtService;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Register a new user. Assumes the caller already validated the DTO.
     * Password hashing is done with injected PasswordEncoder.
     */
    public AuthResponseDto register(AuthRegisterCommand cmd) {
        // Service assumes controller validated cmd (not null, constraints satisfied)

        // Check uniqueness of email
        Optional<User> existing = userRepository.findByEmail(cmd.getEmail().trim().toLowerCase());
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
        }

        // Hash password using injected PasswordEncoder (BCrypt)
        String hashed = passwordEncoder.encode(cmd.getPassword());

        User user = new User();
        user.setEmail(cmd.getEmail().trim().toLowerCase());
        user.setPassword(hashed);
        user.setUserName(cmd.getUserName().trim());
        user.setRole(UserRole.STUDENT);
        user.setActive(true);

        userRepository.save(user);

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresInSeconds = jwtService.getAccessTokenExpirationMs() / 1000L;

        return new AuthResponseDto(accessToken, expiresInSeconds, refreshToken);
    }

    /**
     * Authenticate user credentials and return JWT token pair.
     */
    public AuthResponseDto login(AuthLoginCommand cmd) {
        if (cmd == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        String email = cmd.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(cmd.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresInSeconds = jwtService.getAccessTokenExpirationMs() / 1000L;

        return new AuthResponseDto(accessToken, expiresInSeconds, refreshToken);
    }

    /**
     * Refresh access token using a refresh token. Validates the token type and subject, then issues new tokens.
     */
    public AuthResponseDto refresh(AuthRefreshCommand cmd) {
        String refreshToken = cmd.getRefreshToken().trim();

        // parseClaims will throw ResponseStatusException(401) when token invalid/expired
        Claims claims = jwtService.parseClaims(refreshToken);

        Object typeObj = claims.get("type");
        if (typeObj == null || !"refresh".equals(typeObj.toString())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided token is not a refresh token");
        }

        String subject = claims.getSubject();
        if (subject == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token subject");
        }

        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token subject");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Issue new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        long expiresInSeconds = jwtService.getAccessTokenExpirationMs() / 1000L;

        return new AuthResponseDto(newAccessToken, expiresInSeconds, newRefreshToken);
    }
}
