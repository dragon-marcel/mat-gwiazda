package pl.matgwiazda.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.matgwiazda.dto.TaskDto;
import pl.matgwiazda.dto.TaskGenerateCommand;
import pl.matgwiazda.service.TaskService;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Generate a task (persisted). Returns 201 Created with body.
     */
    @PostMapping(path = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskDto> generateTask(@Valid @RequestBody(required = true) TaskGenerateCommand cmd) {
        TaskDto generated = taskService.generateTask(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(generated);
    }

    /**
     * List tasks with optional filters and pagination.
     * Query params: ?level=1&isActive=true&createdById={uuid}&page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<Page<TaskDto>> listTasks(
            @RequestParam(name = "level", required = false) Short level,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "createdById", required = false) UUID createdById,
            Pageable pageable
    ) {
        Page<TaskDto> page = taskService.listTasks(level, isActive, createdById, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Get a single active task by id.
     */
    @GetMapping(path = "/{taskId}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable("taskId") UUID taskId) {
        TaskDto dto = taskService.getTaskById(taskId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Patch/update an existing task partially.
     */
    // ...existing PATCH endpoint remains unchanged in service layer
}
