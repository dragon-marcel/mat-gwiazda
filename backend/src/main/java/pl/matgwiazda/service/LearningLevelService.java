package pl.matgwiazda.service;

import org.springframework.stereotype.Service;
import pl.matgwiazda.domain.entity.LearningLevel;
import pl.matgwiazda.dto.CreateLearningLevelCommand;
import pl.matgwiazda.dto.LearningLevelDto;
import pl.matgwiazda.dto.UpdateLearningLevelCommand;
import pl.matgwiazda.repository.LearningLevelRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LearningLevelService {

    private final LearningLevelRepository repository;

    public LearningLevelService(LearningLevelRepository repository) {
        this.repository = repository;
    }

    public List<LearningLevelDto> listAll() {
        return repository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public LearningLevelDto getByLevel(short level) {
        LearningLevel ll = repository.findById(level).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning level not found"));
        return toDto(ll);
    }

    public LearningLevelDto create(CreateLearningLevelCommand cmd, UUID actor) {
        short level = cmd.getLevel();
        if (repository.existsById(level)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Learning level already exists");
        }
        LearningLevel ent = new LearningLevel();
        ent.setLevel((short) level);
        ent.setTitle(cmd.getTitle().trim());
        ent.setDescription(cmd.getDescription().trim());
        ent.setCreatedAt(Instant.now());
        ent.setCreatedBy(actor);
        repository.save(ent);
        return toDto(ent);
    }

    public LearningLevelDto update(short level, UpdateLearningLevelCommand cmd, UUID actor) {
        LearningLevel ent = repository.findById(level).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning level not found"));
        boolean changed = false;
        if (cmd.getTitle() != null) {
            ent.setTitle(cmd.getTitle().trim());
            changed = true;
        }
        if (cmd.getDescription() != null) {
            ent.setDescription(cmd.getDescription().trim());
            changed = true;
        }
        if (changed) {
            ent.setModifiedAt(Instant.now());
            ent.setModifiedBy(actor);
            repository.save(ent);
        }
        return toDto(ent);
    }

    public void delete(short level) {
        if (!repository.existsById(level)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning level not found");
        repository.deleteById(level);
    }

    private LearningLevelDto toDto(LearningLevel ent) {
        return new LearningLevelDto(
                ent.getLevel(),
                ent.getTitle(),
                ent.getDescription(),
                ent.getCreatedBy(),
                ent.getCreatedAt(),
                ent.getModifiedBy(),
                ent.getModifiedAt()
        );
    }
}

