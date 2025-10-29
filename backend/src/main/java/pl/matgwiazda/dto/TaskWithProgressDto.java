package pl.matgwiazda.dto;

import java.util.UUID;

public class TaskWithProgressDto {
    private TaskDto task;
    private UUID progressId;

    public TaskWithProgressDto() {}

    public TaskWithProgressDto(TaskDto task, UUID progressId) {
        this.task = task;
        this.progressId = progressId;
    }

    public TaskDto getTask() { return task; }
    public void setTask(TaskDto task) { this.task = task; }

    public UUID getProgressId() { return progressId; }
    public void setProgressId(UUID progressId) { this.progressId = progressId; }
}

