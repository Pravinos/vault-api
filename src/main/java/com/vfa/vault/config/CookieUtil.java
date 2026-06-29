package com.vfa.vault.config;

import java.time.Duration;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CookieUtil {

    private static final Logger log = LoggerFactory.getLogger(CookieUtil.class);

    @Value("${vault.auth.cookie-name}")
    private String cookieName;

    @Value("${vault.auth.cookie-secure}")
    private boolean configuredSecure;

    @Value("${vault.auth.cookie-same-site}")
    private String configuredSameSite;

    @Value("${vault.auth.jwt-expiry-hours}")
    private long expiryHours;

    public ResponseCookie buildTokenCookie(String token, HttpServletRequest request) {
        CookieFlags flags = resolveCookieFlags(request);

        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(flags.secure())
                .path("/")
                .maxAge(Duration.ofHours(expiryHours))
                .sameSite(flags.sameSite())
                .build();
    }

    public ResponseCookie buildClearCookie(HttpServletRequest request) {
        CookieFlags flags = resolveCookieFlags(request);

        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(flags.secure())
                .path("/")
                .maxAge(0)
                .sameSite(flags.sameSite())
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

    private CookieFlags resolveCookieFlags(HttpServletRequest request) {
        boolean secureFlag = resolveSecureFlag(request);
        String sameSiteValue = secureFlag ? configuredSameSite : "Lax";
        return new CookieFlags(secureFlag, sameSiteValue);
    }

    private boolean resolveSecureFlag(HttpServletRequest request) {
        if (request.isSecure() || "true".equalsIgnoreCase(System.getenv("VAULT_COOKIE_FORCE_SECURE"))) {
            return true;
        }

        String env = System.getenv("VAULT_COOKIE_SECURE");
        if (env != null && env.equalsIgnoreCase("false")) {
            return false;
        }

        if (configuredSecure) {
            log.warn(
                    "Request is HTTP but vault.auth.cookie-secure is true; browser may not accept the cookie");
        }

        return configuredSecure;
    }

    private record CookieFlags(boolean secure, String sameSite) {}
}
