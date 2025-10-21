package pl.matgwiazda.domain.enums;

public enum UserRole {
    STUDENT,
    ADMIN;

    public static UserRole fromString(String s) {
        if (s == null) return null;
        try {
            return UserRole.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
