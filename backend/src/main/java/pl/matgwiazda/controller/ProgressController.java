package pl.matgwiazda.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.matgwiazda.dto.ProgressDto;
import pl.matgwiazda.dto.ProgressSubmitCommand;
import pl.matgwiazda.dto.ProgressSubmitResponseDto;
import pl.matgwiazda.service.ProgressService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/progress", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProgressController {

    private final ProgressService progressService;

    @Autowired
    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    /**
     * Submit progress for a task attempt.
     * Expects X-User-Id header containing UUID of the user submitting the progress.
     */
    @PostMapping(path = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProgressSubmitResponseDto> submitProgress(
            @RequestHeader(name = "X-User-Id", required = true) UUID userId,
            @Valid @RequestBody ProgressSubmitCommand cmd
    ) {
        ProgressSubmitResponseDto resp = progressService.submitProgress(userId, cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * List ALL progress attempts for the current user (no pagination).
     * Returns all progress entries for the user without filters.
     */
    @GetMapping(path = "/all")
    public ResponseEntity<List<ProgressDto>> listAllProgress(
            @RequestHeader(name = "X-User-Id", required = true) UUID userId
    ) {
        List<ProgressDto> items = progressService.listAllProgress(userId);
        return ResponseEntity.ok(items);
    }
}
