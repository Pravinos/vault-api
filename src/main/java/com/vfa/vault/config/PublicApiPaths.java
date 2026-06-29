package com.vfa.vault.config;

import java.util.Set;

public final class PublicApiPaths {

    public static final String AUTH_STATUS = "/api/v1/auth/status";
    public static final String AUTH_SETUP = "/api/v1/auth/setup";
    public static final String AUTH_LOGIN = "/api/v1/auth/login";
    public static final String AUTH_RESET_PASSWORD = "/api/v1/auth/reset-password";
    public static final String AUTH_CHANGE_PASSWORD = "/api/v1/auth/change-password";

    private static final Set<String> JWT_SKIP_PATHS = Set.of(
            AUTH_STATUS, AUTH_SETUP, AUTH_LOGIN, AUTH_RESET_PASSWORD);

    private static final Set<String> RATE_LIMITED_AUTH_POST_PATHS = Set.of(
            AUTH_LOGIN, AUTH_SETUP, AUTH_RESET_PASSWORD, AUTH_CHANGE_PASSWORD);

    public static boolean isJwtPublicPath(String path) {
        return JWT_SKIP_PATHS.contains(path);
    }

    public static boolean isRateLimitedAuthPostPath(String path) {
        return RATE_LIMITED_AUTH_POST_PATHS.contains(path);
    }

    private PublicApiPaths() {}
}
