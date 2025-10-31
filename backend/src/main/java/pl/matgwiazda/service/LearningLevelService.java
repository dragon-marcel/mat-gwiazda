package pl.matgwiazda.service;

import org.springframework.stereotype.Service;
import pl.matgwiazda.domain.entity.LearningLevel;
import pl.matgwiazda.dto.CreateLearningLevelCommand;
import pl.matgwiazda.dto.LearningLevelDto;
import pl.matgwiazda.dto.UpdateLearningLevelCommand;
import pl.matgwiazda.repository.LearningLevelRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pl.matgwiazda.mapper.LearningLevelMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LearningLevelService {

    private final LearningLevelRepository repository;
    private final LearningLevelMapper mapper;

    public LearningLevelService(LearningLevelRepository repository, LearningLevelMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<LearningLevelDto> listAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    public LearningLevelDto getByLevel(short level) {
        LearningLevel ll = repository.findById(level).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning level not found"));
        return mapper.toDto(ll);
    }

    public LearningLevelDto create(CreateLearningLevelCommand cmd, UUID actor) {
        short level = cmd.getLevel();
        if (repository.existsById(level)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Learning level already exists");
        }
        LearningLevel ent = new LearningLevel();
        ent.setLevel(level);
        ent.setTitle(cmd.getTitle().trim());
        ent.setDescription(cmd.getDescription().trim());
        ent.setCreatedAt(Instant.now());
        ent.setCreatedBy(actor);
        repository.save(ent);
        return mapper.toDto(ent);
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
        return mapper.toDto(ent);
    }

    public void delete(short level) {
        if (!repository.existsById(level)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning level not found");
        repository.deleteById(level);
    }
}
