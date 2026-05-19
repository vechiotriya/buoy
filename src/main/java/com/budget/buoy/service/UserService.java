package com.budget.buoy.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.authentication.AuthProvider;
import com.budget.buoy.authentication.User;
import com.budget.buoy.authentication.UserRepository;
import com.budget.buoy.exception.InvalidGoogleTokenException;
import com.budget.buoy.exception.ProviderMismatchException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Value;

@Service
public class UserService {

    private final UserRepository userRepository;
    @Value("${google.client-id.web}")
    private String googleClientId;
    @Value("${google.client-id.android}")
    private String androidClientId;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findOrCreateGoogleUser(String idToken) {
        GoogleIdToken.Payload payload = verifyGoogleToken(idToken);

        String email = payload.getEmail();
        String fullName = (String) payload.get("name");

        return userRepository.findByEmail(email)
                .map(existing -> assertGoogleUser(existing, email))
                .orElseGet(() -> createGoogleUser(email, fullName));
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
    try {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new InvalidGoogleTokenException("Token verification failed");
        }

        Map<String, Object> body = response.getBody();

        // If Google returned 200 with an email, the token is genuine
        if (body.get("email") == null) {
            throw new InvalidGoogleTokenException("No email in token");
        }

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail((String) body.get("email"));
        payload.set("name", body.get("name"));
        return payload;

    } catch (InvalidGoogleTokenException e) {
        throw e;
    } catch (Exception e) {
        logger.error("Token verification error: {}", e.getMessage());
        throw new InvalidGoogleTokenException("Could not verify Google token");
    }
}

    private User assertGoogleUser(User existing, String email) {
        // Prevent account hijacking: don't let Google auth take over a LOCAL account
        if (existing.provider() != AuthProvider.GOOGLE) {
            throw new ProviderMismatchException(
                    "Account with this email uses password login");
        }
        return existing;
    }

    private User createGoogleUser(String email, String fullName) {
        String id = NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 12);
        // Derive a username from the email prefix, ensure uniqueness
        String baseUsername = email.split("@")[0];
        String username = resolveUniqueUsername(baseUsername);

        User user = new User(id, fullName, username, email,
                null, AuthProvider.GOOGLE, BigDecimal.ZERO, null);
        return userRepository.save(user);
    }

    private String resolveUniqueUsername(String base) {
        if (!userRepository.existsByUsername(base))
            return base;
        // Append random suffix until unique
        String candidate;
        do {
            candidate = base + NanoIdUtils.randomNanoId(new SecureRandom(), "0123456789".toCharArray(), 4);
        } while (userRepository.existsByUsername(candidate));
        return candidate;
    }
}