package pl.matgwiazda.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command to submit an answer for a previously assigned progress record.
 * The client receives a `progressId` when generating a task and must submit against it.
 */
public class ProgressSubmitCommand {
    @NotNull(message = "progressId is required")
    private UUID progressId;

    @NotNull(message = "selectedOptionIndex is required")
    private Short selectedOptionIndex;

    private Integer timeTakenMs;

    public ProgressSubmitCommand() {
    }

    public ProgressSubmitCommand(UUID progressId, Short selectedOptionIndex, Integer timeTakenMs) {
        this.progressId = progressId;
        this.selectedOptionIndex = selectedOptionIndex;
        this.timeTakenMs = timeTakenMs;
    }

    public UUID getProgressId() {
        return progressId;
    }

    public void setProgressId(UUID progressId) {
        this.progressId = progressId;
    }

    public Short getSelectedOptionIndex() {
        return selectedOptionIndex;
    }

    public void setSelectedOptionIndex(Short selectedOptionIndex) {
        this.selectedOptionIndex = selectedOptionIndex;
    }

    public Integer getTimeTakenMs() {
        return timeTakenMs;
    }

    public void setTimeTakenMs(Integer timeTakenMs) {
        this.timeTakenMs = timeTakenMs;
    }
}
