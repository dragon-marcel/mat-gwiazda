package pl.matgwiazda.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matgwiazda.dto.TaskDto;
import pl.matgwiazda.dto.TaskGenerateCommand;
import pl.matgwiazda.dto.TaskWithProgressDto;
import pl.matgwiazda.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    @Test
    void generateTaskShouldReturnCreated() {
        UUID userId = UUID.randomUUID();
        TaskGenerateCommand cmd = new TaskGenerateCommand();
        cmd.setLevel((short)1);

        TaskWithProgressDto dto = new TaskWithProgressDto();
        // TaskWithProgressDto exposes progressId (not taskId)
        dto.setProgressId(UUID.randomUUID());

        when(taskService.generateTask(cmd, userId)).thenReturn(dto);

        var response = taskController.generateTask(userId, cmd);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(dto, response.getBody());
        verify(taskService).generateTask(cmd, userId);
    }

    @Test
    void listTasksShouldReturnPage() {
        TaskDto t = new TaskDto();
        t.setId(UUID.randomUUID());
        Page<TaskDto> page = new PageImpl<>(List.of(t));

        when(taskService.listTasks(null, null, null, null)).thenReturn(page);

        var response = taskController.listTasks(null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(page, response.getBody());
        verify(taskService).listTasks(null, null, null, null);
    }

    @Test
    void getTaskByIdShouldReturnTask() {
        UUID id = UUID.randomUUID();
        TaskDto dto = new TaskDto();
        dto.setId(id);

        when(taskService.getTaskById(id)).thenReturn(dto);

        var response = taskController.getTaskById(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
        verify(taskService).getTaskById(id);
    }
}
