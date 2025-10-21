package pl.matgwiazda.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Response returned after submitting an answer (POST /api/v1/progress/submit).
 * Combines data from Progress and the resulting User update (points/stars/level).
 */
public class ProgressSubmitResponseDto {
    private UUID progressId;
    private boolean isCorrect;
    private int pointsAwarded;
    private int userPoints;
    private int starsAwarded;
    private boolean leveledUp;
    private short newLevel;
    private String explanation;

    public ProgressSubmitResponseDto() {}

    public ProgressSubmitResponseDto(UUID progressId, boolean isCorrect, int pointsAwarded, int userPoints, int starsAwarded, boolean leveledUp, short newLevel, String explanation) {
        this.progressId = progressId;
        this.isCorrect = isCorrect;
        this.pointsAwarded = pointsAwarded;
        this.userPoints = userPoints;
        this.starsAwarded = starsAwarded;
        this.leveledUp = leveledUp;
        this.newLevel = newLevel;
        this.explanation = explanation;
    }

    public UUID getProgressId() { return progressId; }
    public void setProgressId(UUID progressId) { this.progressId = progressId; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    public int getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(int pointsAwarded) { this.pointsAwarded = pointsAwarded; }

    public int getUserPoints() { return userPoints; }
    public void setUserPoints(int userPoints) { this.userPoints = userPoints; }

    public int getStarsAwarded() { return starsAwarded; }
    public void setStarsAwarded(int starsAwarded) { this.starsAwarded = starsAwarded; }

    public boolean isLeveledUp() { return leveledUp; }
    public void setLeveledUp(boolean leveledUp) { this.leveledUp = leveledUp; }

    public short getNewLevel() { return newLevel; }
    public void setNewLevel(short newLevel) { this.newLevel = newLevel; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgressSubmitResponseDto)) return false;
        ProgressSubmitResponseDto that = (ProgressSubmitResponseDto) o;
        return Objects.equals(progressId, that.progressId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(progressId);
    }
}

