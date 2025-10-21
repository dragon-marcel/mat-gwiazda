package pl.matgwiazda.dto;

/**
 * Response returned by auth endpoints after successful login/register.
 * Contains tokens and optionally user info in the future.
 */
public class AuthResponseDto {
    private String accessToken;
    private long expiresIn;
    private String refreshToken;

    public AuthResponseDto() {}

    public AuthResponseDto(String accessToken, long expiresIn, String refreshToken) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}

