package com.vfa.vault.controller;

import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vfa.vault.config.CookieUtil;
import com.vfa.vault.config.JwtUtil;
import com.vfa.vault.dto.AuthDTO;
import com.vfa.vault.entity.AppConfig;
import com.vfa.vault.repository.AppConfigRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String PASSWORD_HASH_KEY = "vault_password_hash";

    private final AppConfigRepository appConfigRepository;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        boolean configured = false;
        try {
            Optional<AppConfig> config = appConfigRepository.findById(PASSWORD_HASH_KEY);
            if (config.isPresent()) {
                String value = config.get().getValue();
                configured = value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
            }
        } catch (DataAccessException e) {
            configured = false;
        }
        return ResponseEntity.ok(Map.of("configured", configured));
    }

    @PostMapping("/setup")
    public ResponseEntity<Map<String, String>> setup(@RequestBody AuthDTO.PasswordRequest request,
                                                     HttpServletRequest servletRequest,
                                                     HttpServletResponse response) {
        String password = request.password();
        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password must be at least 6 characters"));
        }

        if (appConfigRepository.existsById(PASSWORD_HASH_KEY)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Vault is already configured"));
        }

        AppConfig config = new AppConfig();
        config.setKey(PASSWORD_HASH_KEY);
        config.setValue(passwordEncoder.encode(password));
        appConfigRepository.save(config);

        String token = jwtUtil.generate();
        ResponseCookie cookie = cookieUtil.buildTokenCookie(token, servletRequest);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("message", "Vault configured successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody AuthDTO.PasswordRequest request,
                                                     HttpServletRequest servletRequest,
                                                     HttpServletResponse response) {
        Optional<AppConfig> passwordConfig = appConfigRepository.findById(PASSWORD_HASH_KEY);

        if (passwordConfig.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid password"));
        }

        String hash = passwordConfig.get().getValue();
        if (request.password() != null && passwordEncoder.matches(request.password(), hash)) {
            String token = jwtUtil.generate();
            ResponseCookie cookie = cookieUtil.buildTokenCookie(token, servletRequest);
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return ResponseEntity.ok(Map.of("message", "Login successful"));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid password"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody AuthDTO.ResetPasswordRequest request,
                                                             HttpServletRequest servletRequest,
                                                             HttpServletResponse response) {
        String newPassword = request.newPassword();
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password must be at least 6 characters."));
        }

        // authorize via API_ADMIN_TOKEN or x-reset-token
        String authHeader = servletRequest.getHeader("Authorization");
        String xReset = servletRequest.getHeader("x-reset-token");
        String apiAdmin = System.getenv("API_ADMIN_TOKEN");
        String resetTokenEnv = System.getenv("PASSWORD_RESET_TOKEN");

        boolean authorized = false;
        if (authHeader != null && authHeader.startsWith("Bearer ") && apiAdmin != null) {
            String token = authHeader.substring(7);
            authorized = MessageDigest.isEqual(token.getBytes(), apiAdmin.getBytes());
        }
        if (!authorized && xReset != null && resetTokenEnv != null) {
            authorized = MessageDigest.isEqual(xReset.getBytes(), resetTokenEnv.getBytes());
        }

        if (!authorized) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        AppConfig config = appConfigRepository.findById(PASSWORD_HASH_KEY).orElseGet(() -> {
            AppConfig c = new AppConfig();
            c.setKey(PASSWORD_HASH_KEY);
            return c;
        });

        config.setValue(passwordEncoder.encode(newPassword));
        appConfigRepository.save(config);

        String token = jwtUtil.generate();
        ResponseCookie cookie = cookieUtil.buildTokenCookie(token, servletRequest);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody AuthDTO.ChangePasswordRequest request,
                                                              HttpServletRequest servletRequest,
                                                              HttpServletResponse response) {
        if (request.currentPassword() == null || request.newPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "currentPassword and newPassword are required."));
        }

        if (request.newPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password must be at least 6 characters."));
        }

        if (request.currentPassword().equals(request.newPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "New password must differ from the current password."));
        }

        Optional<AppConfig> passwordConfig = appConfigRepository.findById(PASSWORD_HASH_KEY);
        if (passwordConfig.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "App not configured"));
        }

        String hash = passwordConfig.get().getValue();
        if (!passwordEncoder.matches(request.currentPassword(), hash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Current password is incorrect."));
        }

        AppConfig config = passwordConfig.get();
        config.setValue(passwordEncoder.encode(request.newPassword()));
        appConfigRepository.save(config);

        String token = jwtUtil.generate();
        ResponseCookie cookie = cookieUtil.buildTokenCookie(token, servletRequest);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest servletRequest,
                                                      HttpServletResponse response) {
        ResponseCookie cookie = cookieUtil.buildClearCookie(servletRequest);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verify() {
        return ResponseEntity.ok(Map.of("valid", true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(HttpServletRequest servletRequest,
                                                       HttpServletResponse response) {
        String existing = cookieUtil.extractToken(servletRequest);
        if (existing == null || !jwtUtil.isValid(existing)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No valid token"));
        }

        String token = jwtUtil.generate();
        ResponseCookie cookie = cookieUtil.buildTokenCookie(token, servletRequest);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Token refreshed"));
    }
}