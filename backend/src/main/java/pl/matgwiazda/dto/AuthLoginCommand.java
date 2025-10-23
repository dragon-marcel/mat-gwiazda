package pl.matgwiazda.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Command model for user login (POST /api/v1/auth/login)
 * Maps to User.email and user-supplied pasasword.
 */
public class AuthLoginCommand {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    public AuthLoginCommand() {
    }

    public AuthLoginCommand(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
