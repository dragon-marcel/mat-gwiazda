package pl.matgwiazda.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO representing a user returned by API endpoints.
 * Maps directly to pl.matgwiazda.domain.entity.User fields.
 * Note: `role` is exposed as String (e.g. "STUDENT"), mapping to UserRole enum in service layer.
 */
public class UserDto {
    private UUID id;
    private String email;
    private String userName;
    private String role;
    private short currentLevel;
    private int points;
    private int stars;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastActiveAt;

    public UserDto() {}

    public UserDto(UUID id, String email, String userName, String role, boolean isActive) {
        this(id, email, userName, role, (short)0, 0, 0, isActive, null, null, null);
    }

    public UserDto(UUID id, String email, String userName, String role, short currentLevel, int points, int stars, boolean isActive, Instant createdAt, Instant updatedAt, Instant lastActiveAt) {
        this.id = id;
        this.email = email;
        this.userName = userName;
        this.role = role;
        this.currentLevel = currentLevel;
        this.points = points;
        this.stars = stars;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastActiveAt = lastActiveAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public short getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(short currentLevel) { this.currentLevel = currentLevel; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(Instant lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDto)) return false;
        UserDto userDto = (UserDto) o;
        return Objects.equals(id, userDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
