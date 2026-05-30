package com.budget.buoy.authentication;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("password_reset_otps")
public record PasswordResetOtp(
    @Id Long id,
    String userId,
    String otpHash, 
    Instant expiresAt,
    boolean used
) {}