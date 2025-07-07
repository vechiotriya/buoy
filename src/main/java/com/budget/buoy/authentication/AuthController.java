package com.budget.buoy.authentication;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;
import com.budget.buoy.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public AuthController(TokenService tokenService,AuthenticationManager authenticationManager,UserRepository userRepository,PasswordEncoder passwordEncoder) {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Endpoint for user login
    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
            String token = tokenService.generateToken(authentication);
            return ResponseEntity.ok(token);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    // Endpoint for user signup
        @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody User user) {
        if (userRepository.existsByUsername(user.username())) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        if (userRepository.existsByEmail(user.email())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
      // Encode the password before saving
        user = new User(UUID.randomUUID().toString(),user.username(), user.email(), passwordEncoder.encode(user.password()),new BigDecimal("0.00")
,user.version());
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    // // Endpoint for user logout
    // @PostMapping("/auth/logout")
    // public ResponseEntity<String> logout() {
    //     tokenService.in
    //     log.info("User logged out");
    //     return ResponseEntity.ok("User logged out successfully");
    // }
}
