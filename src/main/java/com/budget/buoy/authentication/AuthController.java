package com.budget.buoy.authentication;

import java.math.BigDecimal;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.exception.InvalidGoogleTokenException;
import com.budget.buoy.exception.ProviderMismatchException;
import com.budget.buoy.service.TokenService;
import com.budget.buoy.service.UserService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    public AuthController(TokenService tokenService,AuthenticationManager authenticationManager,UserRepository userRepository,PasswordEncoder passwordEncoder) {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = new UserService(userRepository);
    }

    // Endpoint for user login
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
            String token = tokenService.generateToken(authentication);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("accessToken", token));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(Map.of("error", "Invalid credentials"));
        }
    }

    // Endpoint for user signup
        @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody User user) {
        if (userRepository.existsByUsername(user.username())) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(Map.of("error", "Username already exists"));
        }

        if (userRepository.existsByEmail(user.email())) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(Map.of("error", "Email already exists"));
        }
      // Encode the password before saving
        user = new User(NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 12),user.fullName(),user.username(),null, user.email(), passwordEncoder.encode(user.password()),AuthProvider.LOCAL,new BigDecimal("0.00")
,null);
        userRepository.save(user);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/auth/google")
public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
    try {
        User user = userService.findOrCreateGoogleUser(request.idToken());

        // Reuse your existing token machinery
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.username(), null, List.of()
        );
        String token = tokenService.generateToken(authentication);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("accessToken", token,"user",user.username()));

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

}
