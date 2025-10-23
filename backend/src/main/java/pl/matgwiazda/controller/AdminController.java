package pl.matgwiazda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.service.AdminService;

import java.util.List;

/**
 * Admin endpoints.
 * Currently protected by checking that the X-User-Id corresponds to a user with role ADMIN.
 */
@RestController
@RequestMapping(path = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Return all users. The caller must provide X-User-Id header of an ADMIN user.
     */
    @GetMapping(path = "/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        List<UserDto> users = adminService.listAllUsers();
        return ResponseEntity.ok(users);
    }
}

