package com.vfa.vault.config;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {

    private static final String WEAK_DEFAULT_SECRET = "please-change-this-secret-to-something-long";
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${vault.auth.jwt-secret}")
    private String secret;

    @Value("${vault.auth.jwt-expiry-hours}")
    private long expiryHours;

    private final Environment environment;

    public JwtUtil(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "vault.auth.jwt-secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }

        boolean isProd = Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equals);
        if (isProd && WEAK_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "vault.auth.jwt-secret must be set via VAULT_JWT_SECRET in production");
        }
    }

    public String generate() {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiryHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject("vault-owner")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
