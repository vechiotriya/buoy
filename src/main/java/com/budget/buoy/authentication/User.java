package com.budget.buoy.authentication;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.Email;

@Table("users")
public record User(
    @Id
    Integer id,
    String username,
    @Email
    String email,
    String password,
    Integer initialBalance,
    @Version
    Integer version    
) {
    public User {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
    }
}
