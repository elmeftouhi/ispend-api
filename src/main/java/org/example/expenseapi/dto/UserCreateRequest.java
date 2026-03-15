package org.example.expenseapi.dto;

import org.example.expenseapi.model.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public class UserCreateRequest {

    @Schema(description = "User's first name", example = "Alice")
    @NotBlank(message = "firstname is required")
    private String firstname;

    @Schema(description = "User's last name", example = "Smith")
    @NotBlank(message = "lastname is required")
    private String lastname;

    @Schema(description = "User's email (used for login)", example = "alice@example.com")
    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @Schema(description = "User's password", example = "P@ssw0rd")
    @NotBlank(message = "password is required")
    @Size(min = 6, message = "password must be at least 6 characters")
    private String password;

    @Schema(description = "Optional initial status for the user")
    private UserStatus status;

    public UserCreateRequest() {}

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
