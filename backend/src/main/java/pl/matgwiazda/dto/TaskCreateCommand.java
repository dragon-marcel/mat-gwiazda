package pl.matgwiazda.dto;

import java.util.List;
import java.util.UUID;

/**
 * Admin command to create a curated task/template.
 * Maps to pl.matgwiazda.domain.entity.Task fields.
 * Note: `options` will be serialized to the entity's jsonb `options` String by the service layer (use Jackson).
 */
public class TaskCreateCommand {
    private short level;
    private String prompt;
    private List<String> options; // must be length 4 (validate in service)
    private short correctOptionIndex;
    private String explanation;
    private UUID createdById; // map to Task.createdBy
    private Boolean isActive;

    public TaskCreateCommand() {}

    public TaskCreateCommand(short level, String prompt, List<String> options, short correctOptionIndex, String explanation, UUID createdById, Boolean isActive) {
        this.level = level;
        this.prompt = prompt;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
        this.createdById = createdById;
        this.isActive = isActive;
    }

    public short getLevel() { return level; }
    public void setLevel(short level) { this.level = level; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public short getCorrectOptionIndex() { return correctOptionIndex; }
    public void setCorrectOptionIndex(short correctOptionIndex) { this.correctOptionIndex = correctOptionIndex; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public UUID getCreatedById() { return createdById; }
    public void setCreatedById(UUID createdById) { this.createdById = createdById; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}

