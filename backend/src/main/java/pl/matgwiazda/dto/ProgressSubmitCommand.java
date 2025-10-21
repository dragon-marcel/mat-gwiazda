package pl.matgwiazda.dto;

import java.util.UUID;

/**
 * Command to submit an answer for a task.
 * Maps to Progress.task (taskId), Progress.selectedOptionIndex and timeTakenMs.
 */
public class ProgressSubmitCommand {
    private UUID taskId;
    private Short selectedOptionIndex;
    private Integer timeTakenMs;

    public ProgressSubmitCommand() {}

    public ProgressSubmitCommand(UUID taskId, Short selectedOptionIndex, Integer timeTakenMs) {
        this.taskId = taskId;
        this.selectedOptionIndex = selectedOptionIndex;
        this.timeTakenMs = timeTakenMs;
    }

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }

    public Short getSelectedOptionIndex() { return selectedOptionIndex; }
    public void setSelectedOptionIndex(Short selectedOptionIndex) { this.selectedOptionIndex = selectedOptionIndex; }

    public Integer getTimeTakenMs() { return timeTakenMs; }
    public void setTimeTakenMs(Integer timeTakenMs) { this.timeTakenMs = timeTakenMs; }
}

