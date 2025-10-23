package pl.matgwiazda.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.UserDto;
import pl.matgwiazda.mapper.UserMapper;
import pl.matgwiazda.repository.UserRepository;

import java.util.List;

/**
 * Service with admin operations.
 * Provides methods that require admin privileges.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public AdminService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Return all users as DTOs. Requires that the caller has ADMIN role.
     *
     * @return list of UserDto representing all users
     */
    public List<UserDto> listAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(userMapper::toDto).toList();
    }
}
