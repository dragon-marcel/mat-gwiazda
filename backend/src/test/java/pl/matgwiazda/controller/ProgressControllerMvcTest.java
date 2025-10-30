package pl.matgwiazda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import pl.matgwiazda.domain.entity.User;
import pl.matgwiazda.domain.enums.UserRole;
import pl.matgwiazda.dto.ProgressDto;
import pl.matgwiazda.dto.ProgressSubmitCommand;
import pl.matgwiazda.dto.ProgressSubmitResponseDto;
import pl.matgwiazda.service.ProgressService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ProgressController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgressControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProgressService progressService;

    private User basicUser(UUID id) {
        User u = new User();
        u.setId(id);
        u.setEmail("stu@example.com");
        u.setUserName("student");
        u.setRole(UserRole.STUDENT);
        return u;
    }

    private UsernamePasswordAuthenticationToken authFor(User user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return new UsernamePasswordAuthenticationToken(user, null, authorities);
    }

    @Test
    void submitProgressShouldReturnCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = basicUser(userId);

        ProgressSubmitCommand cmd = new ProgressSubmitCommand();
        cmd.setProgressId(UUID.randomUUID());
        cmd.setSelectedOptionIndex((short) 1);

        ProgressSubmitResponseDto resp = new ProgressSubmitResponseDto();
        resp.setProgressId(UUID.randomUUID());

        when(progressService.submitProgress(any(java.util.UUID.class), any(ProgressSubmitCommand.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/progress/submit")
                        .with(authentication(authFor(user)))
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progressId").isNotEmpty());

        verify(progressService).submitProgress(any(java.util.UUID.class), any(ProgressSubmitCommand.class));
    }

    @Test
    void listAllProgressShouldReturnList() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = basicUser(userId);

        ProgressDto p = new ProgressDto();
        p.setId(UUID.randomUUID());
        List<ProgressDto> list = List.of(p);

        when(progressService.listAllProgress(any(java.util.UUID.class))).thenReturn(list);

        mockMvc.perform(get("/api/v1/progress/all")
                        .with(authentication(authFor(user)))
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty());

        verify(progressService).listAllProgress(any(java.util.UUID.class));
    }
}
