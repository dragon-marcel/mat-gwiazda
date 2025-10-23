package pl.matgwiazda.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.dto.AuthLoginCommand;
import pl.matgwiazda.dto.AuthRefreshCommand;
import pl.matgwiazda.dto.AuthRegisterCommand;
import pl.matgwiazda.dto.AuthResponseDto;
import pl.matgwiazda.service.AuthService;

/**
 * Authentication endpoints (register/login). Keep simple for now.
 */
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     */
    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody AuthRegisterCommand cmd) {
        if (cmd == null) {
            // In practice Spring will return 400 for a missing body, but keep explicit check for clarity
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        AuthResponseDto resp = authService.register(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * Login and obtain JWT tokens.
     */
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthLoginCommand cmd) {
        AuthResponseDto resp = authService.login(cmd);
        return ResponseEntity.ok(resp);
    }

    /**
     * Refresh access and refresh tokens using a refresh token.
     */
    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody(required = true) AuthRefreshCommand cmd) {
        AuthResponseDto resp = authService.refresh(cmd);
        return ResponseEntity.ok(resp);
    }
}
