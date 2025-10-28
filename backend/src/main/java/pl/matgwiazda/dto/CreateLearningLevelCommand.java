package pl.matgwiazda.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateLearningLevelCommand {

    @NotNull
    @Min(1)
    @Max(8)
    private Short level;

    @NotBlank
    @Size(max = 128)
    private String title;

    @NotBlank
    private String description;

    public CreateLearningLevelCommand() {}

    public CreateLearningLevelCommand(Short level, String title, String description) {
        this.level = level;
        this.title = title;
        this.description = description;
    }

    public Short getLevel() { return level; }
    public void setLevel(Short level) { this.level = level; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

