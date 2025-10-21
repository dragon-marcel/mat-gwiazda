package pl.matgwiazda.dto;

/**
 * Command for patching task metadata (PATCH /api/v1/tasks/{taskId}).
 * Only non-null fields should be applied by the service layer.
 */
public class TaskPatchCommand {
    private String explanation;
    private Boolean isActive;

    public TaskPatchCommand() {}

    public TaskPatchCommand(String explanation, Boolean isActive) {
        this.explanation = explanation;
        this.isActive = isActive;
    }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}

