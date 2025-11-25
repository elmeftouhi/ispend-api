package org.example.expenseapi.testutil;

import org.example.expenseapi.model.User;
import org.example.expenseapi.model.UserStatus;

import java.time.Instant;

public class UserBuilder {
    private Long id;
    private String firstname = "First";
    private String lastname = "Last";
    private String email = "user@example.com";
    private String password = "password";
    private UserStatus status = UserStatus.ACTIVE;

    // audit fields
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public UserBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public UserBuilder firstname(String firstname) {
        this.firstname = firstname;
        return this;
    }

    public UserBuilder lastname(String lastname) {
        this.lastname = lastname;
        return this;
    }

    public UserBuilder email(String email) {
        this.email = email;
        return this;
    }

    public UserBuilder password(String password) {
        this.password = password;
        return this;
    }

    public UserBuilder status(UserStatus status) {
        this.status = status;
        return this;
    }

    // audit setters
    public UserBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public UserBuilder createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public UserBuilder updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public UserBuilder updatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }

    // New fluent aliases using "withX" naming
    public UserBuilder withId(Long id) { return id(id); }
    public UserBuilder withFirstname(String firstname) { return firstname(firstname); }
    public UserBuilder withLastname(String lastname) { return lastname(lastname); }
    public UserBuilder withEmail(String email) { return email(email); }
    public UserBuilder withPassword(String password) { return password(password); }
    public UserBuilder withStatus(UserStatus status) { return status(status); }

    // audit fluent aliases
    public UserBuilder withCreatedAt(Instant createdAt) { return createdAt(createdAt); }
    public UserBuilder withCreatedBy(String createdBy) { return createdBy(createdBy); }
    public UserBuilder withUpdatedAt(Instant updatedAt) { return updatedAt(updatedAt); }
    public UserBuilder withUpdatedBy(String updatedBy) { return updatedBy(updatedBy); }

    // Also provide camel-cased variants in case tests use withFirstName / withLastName
    public UserBuilder withFirstName(String firstName) { return withFirstname(firstName); }
    public UserBuilder withLastName(String lastName) { return withLastname(lastName); }

    public User build() {
        User u = new User();
        if (this.id != null) {
            u.setId(this.id);
        }
        u.setFirstname(this.firstname);
        u.setLastname(this.lastname);
        u.setEmail(this.email);
        u.setPassword(this.password);
        u.setStatus(this.status);
        // set audit fields if provided
        if (this.createdAt != null) u.setCreatedAt(this.createdAt);
        if (this.createdBy != null) u.setCreatedBy(this.createdBy);
        if (this.updatedAt != null) u.setUpdatedAt(this.updatedAt);
        if (this.updatedBy != null) u.setUpdatedBy(this.updatedBy);
        return u;
    }
}
