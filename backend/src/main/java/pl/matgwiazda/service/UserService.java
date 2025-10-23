package pl.matgwiazda.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.dto.UserUpdateCommand;
import pl.matgwiazda.mapper.UserMapper;
import pl.matgwiazda.repository.UserRepository;

import java.util.UUID;

@Service
public class UserService {

    private static final String USER_NOT_FOUND = "User not found";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public UserDto getUserDtoById(UUID id) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        return userMapper.toDto(u);
    }

    public UserDto getUserDtoFromEntity(User u) {
        return userMapper.toDto(u);
    }

    public UserDto updateUser(UUID id, UserUpdateCommand cmd) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        boolean changed = false;
        if (cmd.getUserName() != null) {
            u.setUserName(cmd.getUserName().trim());
            changed = true;
        }
        if (cmd.getPassword() != null) {
            String hashed = passwordEncoder.encode(cmd.getPassword());
            u.setPassword(hashed);
            changed = true;
        }
        if (changed) {
            userRepository.save(u);
        }
        return userMapper.toDto(u);
    }

    public void deactivateUser(UUID id) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        u.setActive(false);
        userRepository.save(u);
    }
}
