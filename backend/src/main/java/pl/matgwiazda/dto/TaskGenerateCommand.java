package pl.matgwiazda.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command for generating a task.
 */
public class TaskGenerateCommand {

    @NotNull(message = "level is required")
    @Min(value = 1, message = "level must be at least 1")
    @Max(value = 8, message = "level must be at most 8")
    private Short level; // required, 1..8

    private UUID createdById; // optional

    public TaskGenerateCommand() {
    }

    public Short getLevel() {
        return level;
    }

    public void setLevel(Short level) {
        this.level = level;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public void setCreatedById(UUID createdById) {
        this.createdById = createdById;
    }
}
