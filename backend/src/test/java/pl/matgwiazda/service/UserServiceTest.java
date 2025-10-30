package pl.matgwiazda.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.dto.UserUpdateCommand;
import pl.matgwiazda.mapper.UserMapper;
import pl.matgwiazda.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    UserMapper userMapper;

    @InjectMocks
    UserService userService;

    User sample;

    @BeforeEach
    void setUp() {
        sample = new User();
        sample.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        sample.setEmail("a@b.com");
        sample.setUserName("u");
        sample.setPassword("old");
        sample.setActive(true);
    }

    @Test
    void getUserDtoById_found_returnsDto() {
        when(userRepository.findById(sample.getId())).thenReturn(Optional.of(sample));
        when(userMapper.toDto(sample)).thenReturn(new UserDto(sample.getId(), sample.getEmail(), sample.getUserName(), null, sample.isActive()));

        UserDto dto = userService.getUserDtoById(sample.getId());

        assertThat(dto.getId()).isEqualTo(sample.getId());
        verify(userRepository).findById(sample.getId());
    }

    @Test
    void getUserDtoById_notFound_throws() {
        when(userRepository.findById(sample.getId())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> userService.getUserDtoById(sample.getId()));
    }

    @Test
    void updateUser_changesUsernameAndPassword_and_saves() {
        when(userRepository.findById(sample.getId())).thenReturn(Optional.of(sample));
        when(passwordEncoder.encode("newpass")).thenReturn("hashed");
        when(userMapper.toDto(any())).thenReturn(new UserDto(sample.getId(), sample.getEmail(), "new", null, true));

        UserUpdateCommand cmd = new UserUpdateCommand();
        cmd.setUserName(" new ");
        cmd.setPassword("newpass");

        UserDto dto = userService.updateUser(sample.getId(), cmd);

        assertThat(dto.getUserName()).isEqualTo("new");
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().getPassword()).isEqualTo("hashed");
    }

    @Test
    void updateUser_notFound_throws() {
        when(userRepository.findById(sample.getId())).thenReturn(Optional.empty());
        UserUpdateCommand cmd = new UserUpdateCommand();
        assertThrows(ResponseStatusException.class, () -> userService.updateUser(sample.getId(), cmd));
    }

    @Test
    void deactivateUser_setsActiveFalse_and_saves() {
        when(userRepository.findById(sample.getId())).thenReturn(Optional.of(sample));

        userService.deactivateUser(sample.getId());

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().isActive()).isFalse();
    }

    @Test
    void deactivateUser_notFound_throws() {
        when(userRepository.findById(sample.getId())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> userService.deactivateUser(sample.getId()));
    }
}

