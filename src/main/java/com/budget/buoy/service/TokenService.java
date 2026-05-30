package com.budget.buoy.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final JwtDecoder decoder;
    private final JwtEncoder encoder;
    public TokenService(JwtEncoder encoder, JwtDecoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        String scope=authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));
        JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("self")
        .issuedAt(now)
        .expiresAt(now.plus(35,ChronoUnit.DAYS))
        .subject(authentication.getName())
        .claim("scope", scope)
        .build();
        return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

     /** Issues a 15-min JWT valid only for password reset. */
    public String generateResetToken(String userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .subject(userId)
                .claim("purpose", "password_reset")   // scope lock
                .build();
        return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

        /**
     * Validates a reset token.
     * Returns the userId (subject) if valid, empty if expired / wrong purpose / tampered.
     */
    public Optional<String> validateResetToken(String token) {
        try {
            Jwt jwt = decoder.decode(token);

            // Reject tokens not issued specifically for reset
            if (!"password_reset".equals(jwt.getClaim("purpose"))) {
                return Optional.empty();
            }
            // Spring's decoder already checks expiry — reaching here means it's valid
            return Optional.of(jwt.getSubject());

        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}
