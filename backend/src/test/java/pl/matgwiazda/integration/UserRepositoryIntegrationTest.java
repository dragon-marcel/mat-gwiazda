package pl.matgwiazda.integration;

// ...existing code...

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByEmail_shouldWork() {
        User u = new User();
        u.setEmail("itest@example.com");
        u.setPassword("secret");
        u.setUserName("itest");

        User saved = userRepository.save(u);
        assertThat(saved.getId()).isNotNull();

        Optional<User> found = userRepository.findByEmail("itest@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUserName()).isEqualTo("itest");
    }
}
