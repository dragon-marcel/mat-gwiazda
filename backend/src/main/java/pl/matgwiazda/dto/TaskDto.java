package pl.matgwiazda.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO representing a Task instance returned by the API.
 * Notes:
 * - `options` should be converted from Task.options (jsonb String in entity) to List<String> by service/controller using Jackson or a small util.
 * - `correctOptionIndex` is nullable and should only be populated for admin endpoints; do NOT expose it for regular user responses.
 * - `createdById` maps to Task.createdBy.id when present.
 */
public class TaskDto {
    private UUID id;
    private short level;
    private String prompt;
    private List<String> options;
    private Integer correctOptionIndex; // nullable; admin-only
    private String explanation; // may be omitted for challenge flow
    private UUID createdById;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public TaskDto() {}

    public TaskDto(UUID id, short level, String prompt, List<String> options, Integer correctOptionIndex, String explanation, UUID createdById, boolean isActive, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.level = level;
        this.prompt = prompt;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
        this.createdById = createdById;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public short getLevel() { return level; }
    public void setLevel(short level) { this.level = level; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public Integer getCorrectOptionIndex() { return correctOptionIndex; }
    public void setCorrectOptionIndex(Integer correctOptionIndex) { this.correctOptionIndex = correctOptionIndex; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public UUID getCreatedById() { return createdById; }
    public void setCreatedById(UUID createdById) { this.createdById = createdById; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskDto)) return false;
        TaskDto taskDto = (TaskDto) o;
        return Objects.equals(id, taskDto.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

