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

    public ResponseCookie buildTokenCookie(String token, jakarta.servlet.http.HttpServletRequest request) {
        boolean isSecure = request.isSecure() || "true".equalsIgnoreCase(System.getenv("VAULT_COOKIE_FORCE_SECURE"));

        boolean secureFlag = isSecure;
        String env = System.getenv("VAULT_COOKIE_SECURE");
        if (env != null && env.equalsIgnoreCase("false") && !request.isSecure()) {
            secureFlag = false;
        } else if ((env == null || env.equalsIgnoreCase("true")) && !request.isSecure()) {
            // running over HTTP but env requests secure cookies: warn and keep secure=true for safety
            if (secureFlag) {
                System.err.println("WARNING: Request is HTTP but VAULT_COOKIE_SECURE is true; sending Secure cookie.");
            }
        }

        String sameSiteValue = secureFlag ? "None" : "Lax";

        return ResponseCookie.from(cookieName, token)
            .httpOnly(true)
            .secure(secureFlag)
            .path("/")
            .maxAge(Duration.ofHours(expiryHours))
            .sameSite(sameSiteValue)
            .build();
    }

    public ResponseCookie buildClearCookie(jakarta.servlet.http.HttpServletRequest request) {
        boolean isSecure = request.isSecure() || "true".equalsIgnoreCase(System.getenv("VAULT_COOKIE_FORCE_SECURE"));

        boolean secureFlag = isSecure;
        String env = System.getenv("VAULT_COOKIE_SECURE");
        if (env != null && env.equalsIgnoreCase("false") && !request.isSecure()) {
            secureFlag = false;
        }

        String sameSiteValue = secureFlag ? "None" : "Lax";

        return ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(secureFlag)
            .path("/")
            .maxAge(0)
            .sameSite(sameSiteValue)
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