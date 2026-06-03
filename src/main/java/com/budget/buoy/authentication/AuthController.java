package com.budget.buoy.authentication;

import com.budget.buoy.service.InstagramService;
import com.budget.buoy.service.PasswordResetService;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.exception.InvalidGoogleTokenException;
import com.budget.buoy.exception.PasswordResetNotSupportedException;
import com.budget.buoy.exception.ProviderMismatchException;
import com.budget.buoy.service.TokenService;
import com.budget.buoy.service.UserService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class AuthController {
    private final PasswordResetService passwordResetService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final InstagramService instagramService;
    private final PasswordEncoder passwordEncoder;
    @Value("${INSTAGRAM_VERIFY_TOKEN}")
    private String verifyToken;

    public AuthController(TokenService tokenService, AuthenticationManager authenticationManager,
            UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordResetService passwordResetService,
            InstagramService instagramService) {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.instagramService = new InstagramService();
        this.userService = new UserService(userRepository, instagramService);
        this.passwordResetService = passwordResetService;
    }

    // Endpoint for user login
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .or(() -> userRepository.findByEmail(authRequest.getUsername()))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.username(), authRequest.getPassword()));
            String token = tokenService.generateToken(authentication);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("accessToken", token));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    // Endpoint for user signup
    @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody User user) {
        if (userRepository.existsByUsername(user.username())) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Username already exists"));
        }

        if (userRepository.existsByEmail(user.email())) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Email already exists"));
        }
        // Encode the password before saving
        user = new User(NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 12), user.fullName(),
                user.username(), null, user.email(), passwordEncoder.encode(user.password()), AuthProvider.LOCAL,
                user.balance(), null);
        userRepository.save(user);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/auth/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        try {
            User user = userService.findOrCreateGoogleUser(request.idToken(), request.balance());
            // Reuse your existing token machinery
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.username(), null, List.of());
            String token = tokenService.generateToken(authentication);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("accessToken", token, "user", user.username()));

        } catch (InvalidGoogleTokenException e) {
            return ResponseEntity.status(401)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Invalid Google token"));

        } catch (ProviderMismatchException e) {
            return ResponseEntity.status(409)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/auth/instagram/callback")
    public void callback(
            @RequestParam("code") String code,
            HttpServletResponse response) throws IOException {

        User user = userService.findOrCreateInstagramUser(
                code);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.username(),
                null,
                List.of());

        String jwt = tokenService.generateToken(authentication);

        response.sendRedirect(
                "buoyapp://?token=" +
                        URLEncoder.encode(jwt, StandardCharsets.UTF_8));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        try {
            passwordResetService.initiate(req.email());

            return ResponseEntity.ok(
                    Map.of("message", "If that email exists, a code was sent."));
        } catch (PasswordResetNotSupportedException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/auth/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return passwordResetService.verifyOtp(req.email(), req.otp())
                .map(token -> ResponseEntity.ok(Map.of("resetToken", token)))
                .orElseGet(() -> ResponseEntity.status(400)
                        .body(Map.of("error", "Invalid or expired OTP")));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        boolean ok = passwordResetService.resetPassword(req.resetToken(), req.newPassword());
        if (ok)
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired reset token"));
    }

    @GetMapping("/webhooks/instagram")
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode)
                && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Invalid token");
    }

}
