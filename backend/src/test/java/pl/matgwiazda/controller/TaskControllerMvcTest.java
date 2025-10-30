package pl.matgwiazda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.domain.enums.UserRole;
import pl.matgwiazda.dto.TaskDto;
import pl.matgwiazda.dto.TaskGenerateCommand;
import pl.matgwiazda.dto.TaskWithProgressDto;
import pl.matgwiazda.service.TaskService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    // no security principal needed; controller uses X-User-Id header
    @Test
    void generateTaskShouldReturnCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = basicUser(userId);
        TaskGenerateCommand cmd = new TaskGenerateCommand();
        cmd.setLevel((short) 2);

        TaskWithProgressDto dto = new TaskWithProgressDto();
        dto.setProgressId(UUID.randomUUID());

        when(taskService.generateTask(any(TaskGenerateCommand.class), eq(userId))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/tasks/generate")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progressId").isNotEmpty());

        verify(taskService).generateTask(any(TaskGenerateCommand.class), eq(userId));
    }

    @Test
    void listTasksShouldReturnPageWithContent() throws Exception {
        User user = basicUser(UUID.randomUUID());
        TaskDto t = new TaskDto();
        t.setId(UUID.randomUUID());

        when(taskService.listTasks(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(t)));

        mockMvc.perform(get("/api/v1/tasks"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.content[0].id").isNotEmpty());

        verify(taskService).listTasks(any(), any(), any(), any());
    }

    private User basicUser(UUID id) {
        User u = new User();
        u.setId(id);
        u.setEmail("stu@example.com");
        u.setUserName("student");
        u.setRole(UserRole.STUDENT);
        return u;
    }
}
