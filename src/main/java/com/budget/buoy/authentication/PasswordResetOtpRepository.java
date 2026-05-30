package com.budget.buoy.authentication;

import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface PasswordResetOtpRepository
        extends CrudRepository<PasswordResetOtp, Long> {

    // Latest unused, unexpired OTP for a user
    @Query("""
        SELECT * FROM password_reset_otps
        WHERE user_id = :userId
          AND used = false
          AND expires_at > NOW()
        ORDER BY expires_at DESC
        LIMIT 1
        """)
    Optional<PasswordResetOtp> findActiveByUserId(String userId);

    // Invalidate all OTPs for a user after success / new request
    @Modifying
    @Query("UPDATE password_reset_otps SET used = true WHERE user_id = :userId")
    void invalidateAllForUser(String userId);
}