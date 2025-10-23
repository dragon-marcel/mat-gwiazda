package pl.matgwiazda.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.matgwiazda.domain.entity.Task;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.dto.TaskDto;
import pl.matgwiazda.dto.TaskGenerateCommand;
import pl.matgwiazda.mapper.TaskMapper;
import pl.matgwiazda.repository.TaskRepository;
import pl.matgwiazda.repository.UserRepository;

import java.util.*;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    @Autowired
    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
    }

    public TaskDto generateTask(TaskGenerateCommand cmd) {
        // Controller enforces required @RequestBody and @Valid, so cmd is guaranteed non-null and valid

        // Resolve createdBy if provided
        User createdBy = null;
        if (cmd.getCreatedById() != null) {
            Optional<User> userOpt = userRepository.findById(cmd.getCreatedById());
            if (userOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`createdById` does not reference an existing user");
            }
            createdBy = userOpt.get();
        }

        // Simple generator: arithmetic question influenced by level
        short level = cmd.getLevel();
        int a = level * 2;
        int b = Math.max(1, level + 1);
        int correct = a + b;

        String prompt = String.format("Compute %d + %d", a, b);

        // Create options and shuffle
        List<String> options = new ArrayList<>();
        options.add(String.valueOf(correct));
        options.add(String.valueOf(correct + 1));
        options.add(String.valueOf(Math.max(0, correct - 1)));
        options.add(String.valueOf(correct + 2));
        Collections.shuffle(options);

        int correctIndex = options.indexOf(String.valueOf(correct));

        // Build Task entity
        Task task = new Task();
        task.setLevel(level);
        task.setPrompt(prompt);
        task.setCorrectOptionIndex((short) correctIndex);
        task.setExplanation(null); // no explanation by default for generated tasks
        task.setCreatedBy(createdBy);
        task.setActive(true);
        // entity now stores options as List<String> (jsonb via hibernate-types)
        task.setOptions(options);

        // Persist generated task
        Task saved = taskRepository.save(task);
        return taskMapper.toDto(saved);
    }

    public TaskDto getTaskById(UUID id) {
        Optional<Task> opt = taskRepository.findByIdAndIsActive(id, true);
        Task task = opt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        return taskMapper.toDto(task);
    }

    public Page<TaskDto> listTasks(Short level, Boolean isActive, UUID createdById, Pageable pageable) {
        Page<Task> page;
        if (level != null && createdById != null && isActive != null) {
            page = taskRepository.findByLevelAndCreatedByIdAndIsActive(level, createdById, isActive, pageable);
        } else if (level != null && createdById != null) {
            page = taskRepository.findByLevelAndCreatedById(level, createdById, pageable);
        } else if (level != null && isActive != null) {
            page = taskRepository.findByLevelAndIsActive(level, isActive, pageable);
        } else if (createdById != null && isActive != null) {
            page = taskRepository.findByCreatedByIdAndIsActive(createdById, isActive, pageable);
        } else if (level != null) {
            page = taskRepository.findByLevel(level, pageable);
        } else if (createdById != null) {
            page = taskRepository.findByCreatedById(createdById, pageable);
        } else if (isActive != null) {
            page = taskRepository.findByIsActive(isActive, pageable);
        } else {
            page = taskRepository.findAll(pageable);
        }

        return page.map(taskMapper::toDto);
    }

}
