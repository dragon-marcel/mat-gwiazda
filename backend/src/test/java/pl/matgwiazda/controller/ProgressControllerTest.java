package pl.matgwiazda.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matgwiazda.dto.ProgressDto;
import pl.matgwiazda.dto.ProgressSubmitCommand;
import pl.matgwiazda.dto.ProgressSubmitResponseDto;
import pl.matgwiazda.service.ProgressService;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressControllerTest {

    @Mock
    private ProgressService progressService;

    @InjectMocks
    private ProgressController progressController;

    @Test
    void submitProgressShouldReturnCreated() {
        UUID userId = UUID.randomUUID();
        ProgressSubmitCommand cmd = new ProgressSubmitCommand();
        // ProgressSubmitCommand uses progressId (client receives progressId when task is generated)
        cmd.setProgressId(UUID.randomUUID());

        ProgressSubmitResponseDto resp = new ProgressSubmitResponseDto();
        resp.setProgressId(UUID.randomUUID());

        when(progressService.submitProgress(userId, cmd)).thenReturn(resp);

        var response = progressController.submitProgress(userId, cmd);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(resp, response.getBody());
        verify(progressService).submitProgress(userId, cmd);
    }

    @Test
    void listAllProgressShouldReturnList() {
        UUID userId = UUID.randomUUID();
        ProgressDto p = new ProgressDto();
        p.setId(UUID.randomUUID());
        List<ProgressDto> list = List.of(p);

        when(progressService.listAllProgress(userId)).thenReturn(list);

        var response = progressController.listAllProgress(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(list, response.getBody());
        verify(progressService).listAllProgress(userId);
    }
}
