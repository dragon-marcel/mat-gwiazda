package pl.matgwiazda.dto;

import jakarta.validation.constraints.Size;

/**
 * Command model for partial user updates (PATCH /api/v1/users/me).
 * Only non-null fields should be applied by the service layer.
 */
public class UserUpdateCommand {
    @Size(min = 2, max = 100)
    private String userName;

    @Size(min = 6, max = 255)
    private String password;

    public UserUpdateCommand() {
    }

    public UserUpdateCommand(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
