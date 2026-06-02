package com.budget.buoy.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.budget.buoy.authentication.AuthController;
import com.budget.buoy.authentication.AuthProvider;
import com.budget.buoy.authentication.PasswordResetOtp;
import com.budget.buoy.authentication.PasswordResetOtpRepository;
import com.budget.buoy.authentication.User;
import com.budget.buoy.authentication.UserRepository;

@Service
public class PasswordResetService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 10;

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenService tokenService;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    public PasswordResetService(UserRepository userRepository,
            PasswordResetOtpRepository otpRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            TokenService tokenService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tokenService = tokenService;
    }

    /** Step 1 – unchanged */
    public void initiate(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Guard: Google/OAuth users have no password to reset
            if (user.provider() != AuthProvider.LOCAL)
                return;

            otpRepository.invalidateAllForUser(user.id());

            String otp = generateOtp();
            otpRepository.save(new PasswordResetOtp(
                    null,
                    user.id(),
                    passwordEncoder.encode(otp),
                    Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES),
                    false));
            emailService.sendOtp(user.email(), user.fullName(), otp);
        });
    }

    /**
     * Step 2 – verify OTP → return a signed 15-min reset JWT.
     * The JWT carries the userId; no DB lookup needed at reset time.
     */
    public Optional<String> verifyOtp(String email, String otp) {
        return userRepository.findByEmail(email)
                .flatMap(user -> otpRepository.findActiveByUserId(user.id())
                        .filter(rec -> passwordEncoder.matches(otp, rec.otpHash()))
                        .map(rec -> {
                            otpRepository.invalidateAllForUser(user.id());
                            return tokenService.generateResetToken(user.id()); // ← JWT now
                        }));
    }

    /**
     * Step 3 – validate reset JWT, update password.
     * Email is no longer needed for lookup — userId lives in the token.
     */
    public boolean resetPassword(String resetToken, String newPassword) {
        return tokenService.validateResetToken(resetToken)
                .flatMap(userRepository::findById)
                .map(user -> {
                    User updated = new User(
                            user.id(), user.fullName(), user.username(), user.profile(),
                            user.email(), passwordEncoder.encode(newPassword),
                            user.provider(), user.balance(), user.version());
                    userRepository.save(updated);
                    return true;
                })
                .orElse(false);
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        return String.format("%0" + OTP_LENGTH + "d", secureRandom.nextInt(bound));
    }
}
