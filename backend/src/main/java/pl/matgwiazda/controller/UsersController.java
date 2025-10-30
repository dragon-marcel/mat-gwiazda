package pl.matgwiazda.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.dto.UserUpdateCommand;
import pl.matgwiazda.service.UserService;

import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping(path = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsersController {

    private final UserService userService;

    @Autowired
    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(path = "/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }
        UserDto dto = userService.getUserDtoFromEntity(user);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping(path = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> updateMe(@AuthenticationPrincipal User user, @Valid @RequestBody UserUpdateCommand cmd) {
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }
        UserDto dto = userService.updateUser(user.getId(), cmd);
        return ResponseEntity.ok(dto);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable("id") UUID id) {
        UserDto dto = userService.getUserDtoById(id);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping(path = "/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }
        userService.deactivateUser(user.getId());
        return ResponseEntity.noContent().build();
    }
}
