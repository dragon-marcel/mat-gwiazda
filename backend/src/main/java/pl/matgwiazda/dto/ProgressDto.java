package pl.matgwiazda.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO representing a Progress (attempt) record returned by API.
 * Maps to pl.matgwiazda.domain.entity.Progress fields. Related entities are exposed by id.
 */
public class ProgressDto {
    private UUID id;
    private UUID userId;
    private UUID taskId;
    private int attemptNumber;
    private Short selectedOptionIndex;
    private boolean isCorrect;
    private int pointsAwarded;
    private Integer timeTakenMs;
    private Instant createdAt;
    private Instant updatedAt;

    public ProgressDto() {}

    public ProgressDto(UUID id, UUID userId, UUID taskId, int attemptNumber, Short selectedOptionIndex, boolean isCorrect, int pointsAwarded, Integer timeTakenMs, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.taskId = taskId;
        this.attemptNumber = attemptNumber;
        this.selectedOptionIndex = selectedOptionIndex;
        this.isCorrect = isCorrect;
        this.pointsAwarded = pointsAwarded;
        this.timeTakenMs = timeTakenMs;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }

    public Short getSelectedOptionIndex() { return selectedOptionIndex; }
    public void setSelectedOptionIndex(Short selectedOptionIndex) { this.selectedOptionIndex = selectedOptionIndex; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    public int getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(int pointsAwarded) { this.pointsAwarded = pointsAwarded; }

    public Integer getTimeTakenMs() { return timeTakenMs; }
    public void setTimeTakenMs(Integer timeTakenMs) { this.timeTakenMs = timeTakenMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgressDto)) return false;
        ProgressDto that = (ProgressDto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

