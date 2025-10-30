package pl.matgwiazda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.dto.UserUpdateCommand;
import pl.matgwiazda.repository.UserRepository;
import pl.matgwiazda.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getUserDtoById_returnsDto() {
        User u = new User();
        u.setEmail("userint@example.com");
        u.setPassword("secret");
        u.setUserName("u1");
        User saved = userRepository.save(u);

        UserDto dto = userService.getUserDtoById(saved.getId());
        assertThat(dto).isNotNull();
        assertThat(dto.getEmail()).isEqualTo("userint@example.com");
        assertThat(dto.getUserName()).isEqualTo("u1");
    }

    @Test
    void updateUser_changesUserNameAndPassword() {
        User u = new User();
        u.setEmail("updateint@example.com");
        u.setPassword(passwordEncoder.encode("oldpwd"));
        u.setUserName("oldname");
        User saved = userRepository.save(u);

        UserUpdateCommand cmd = new UserUpdateCommand();
        cmd.setUserName(" newname ");
        cmd.setPassword("newpassword");

        var updated = userService.updateUser(saved.getId(), cmd);
        assertThat(updated.getUserName()).isEqualTo("newname");

        User fromDb = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newpassword", fromDb.getPassword())).isTrue();
    }

    @Test
    void deactivateUser_setsActiveFalse() {
        User u = new User();
        u.setEmail("deact@example.com");
        u.setPassword("p");
        u.setUserName("duser");
        User saved = userRepository.save(u);

        userService.deactivateUser(saved.getId());

        User fromDb = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(fromDb.isActive()).isFalse();
    }
}

