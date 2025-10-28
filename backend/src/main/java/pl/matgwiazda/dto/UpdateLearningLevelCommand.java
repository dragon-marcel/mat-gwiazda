package pl.matgwiazda.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UpdateLearningLevelCommand {
    @Min(1)
    private Short level;

    @Size(max = 128)
    private String title;

    private String description;

    public UpdateLearningLevelCommand() {}

    public UpdateLearningLevelCommand(Short level, String title, String description) {
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

