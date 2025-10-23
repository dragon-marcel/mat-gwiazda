package pl.matgwiazda.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Command model for refreshing auth tokens (POST /api/v1/auth/refresh)
 */
public class AuthRefreshCommand {
    @NotBlank
    private String refreshToken;

    public AuthRefreshCommand() {
    }

    public AuthRefreshCommand(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
