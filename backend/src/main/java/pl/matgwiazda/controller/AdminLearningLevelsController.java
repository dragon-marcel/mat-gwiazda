package pl.matgwiazda.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.matgwiazda.dto.CreateLearningLevelCommand;
import pl.matgwiazda.dto.LearningLevelDto;
import pl.matgwiazda.dto.UpdateLearningLevelCommand;
import pl.matgwiazda.service.LearningLevelService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/admin/learning-levels", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class AdminLearningLevelsController {

    private final LearningLevelService service;

    public AdminLearningLevelsController(LearningLevelService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<LearningLevelDto>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping(path = "/{level}")
    public ResponseEntity<LearningLevelDto> getByLevel(@PathVariable("level") short level) {
        return ResponseEntity.ok(service.getByLevel(level));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LearningLevelDto> create(
            @Valid @RequestBody CreateLearningLevelCommand cmd,
            @RequestHeader(value = "X-User-Id") UUID userId
    ) {
        LearningLevelDto created = service.create(cmd, userId);
        return ResponseEntity.created(URI.create("/api/v1/admin/learning-levels/" + created.level())).body(created);
    }

    @PutMapping(path = "/{level}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LearningLevelDto> update(
            @PathVariable("level") short level,
            @Valid @RequestBody UpdateLearningLevelCommand cmd,
            @RequestHeader(value = "X-User-Id") UUID userId
    ) {
        LearningLevelDto updated = service.update(level, cmd, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping(path = "/{level}")
    public ResponseEntity<Void> delete(@PathVariable("level") short level) {
        service.delete(level);
        return ResponseEntity.noContent().build();
    }
}
