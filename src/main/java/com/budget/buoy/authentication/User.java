package com.budget.buoy.authentication;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

@Table("users")
public record User(
    @Id
    String id,
    String username,
    @Email @NotNull
    String email,
    String password,
    BigDecimal initialBalance,
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
