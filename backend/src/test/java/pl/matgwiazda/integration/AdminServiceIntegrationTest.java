package pl.matgwiazda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.domain.enums.UserRole;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.repository.UserRepository;
import pl.matgwiazda.repository.ProgressRepository;
import pl.matgwiazda.repository.TaskRepository;
import pl.matgwiazda.service.AdminService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AdminServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void listAllUsers_returnsAllUsersAsDtos() {
        // prepare users
        User u1 = new User();
        u1.setEmail("adminint@example.com");
        u1.setPassword("p");
        u1.setUserName("admin-user");
        u1.setRole(UserRole.ADMIN);

        User u2 = new User();
        u2.setEmail("studentint@example.com");
        u2.setPassword("p");
        u2.setUserName("student-user");
        // default role is STUDENT

        userRepository.saveAll(List.of(u1, u2));

        List<UserDto> result = adminService.listAllUsers();

        // Check that our created users are present
        assertThat(result).isNotNull();
        assertThat(result).extracting(UserDto::getEmail)
                .contains("adminint@example.com", "studentint@example.com");

        // role mapping should convert enum to name string
        UserDto adminDto = result.stream()
                .filter(d -> "adminint@example.com".equals(d.getEmail()))
                .findFirst()
                .orElseThrow();
        assertThat(adminDto.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void listAllUsers_returnsEmptyListWhenNoUsers() {
        // delete dependent entities first to avoid FK constraint violations
        progressRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        List<UserDto> result = adminService.listAllUsers();

        assertThat(result).isNotNull().isEmpty();
    }
}
