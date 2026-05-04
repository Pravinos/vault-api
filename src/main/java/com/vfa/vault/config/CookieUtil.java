package com.vfa.vault.config;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CookieUtil {

    @Value("${vault.auth.cookie-name}")
    private String cookieName;

    @Value("${vault.auth.cookie-secure}")
    private boolean secure;

    @Value("${vault.auth.cookie-same-site}")
    private String sameSite;

    @Value("${vault.auth.jwt-expiry-hours}")
    private int expiryHours;

    public ResponseCookie buildTokenCookie(String token) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ofHours(expiryHours))
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie buildClearCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }

    public String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}