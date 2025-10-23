package pl.matgwiazda.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.dto.UserUpdateCommand;
import pl.matgwiazda.service.UserService;

import java.util.UUID;

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
        UserDto dto = userService.getUserDtoFromEntity(user);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping(path = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> updateMe(@AuthenticationPrincipal User user, @Valid @RequestBody UserUpdateCommand cmd) {
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
        userService.deactivateUser(user.getId());
        return ResponseEntity.noContent().build();
    }
}

