package com.vfa.vault.controller;

import com.vfa.vault.config.CookieUtil;
import com.vfa.vault.config.JwtUtil;
import com.vfa.vault.entity.AppConfig;
import com.vfa.vault.repository.AppConfigRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

import java.util.Map;
import java.util.Optional;

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
        boolean configured = appConfigRepository.existsById(PASSWORD_HASH_KEY);
        return ResponseEntity.ok(Map.of("configured", configured));
    }

    @PostMapping("/setup")
    public ResponseEntity<Map<String, String>> setup(@RequestBody PasswordRequest request,
                                                     HttpServletResponse response) {
        if (appConfigRepository.existsById(PASSWORD_HASH_KEY)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Already configured"));
        }

        if (request.password() == null || request.password().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password must be at least 8 characters"));
        }

        AppConfig config = new AppConfig();
        config.setKey(PASSWORD_HASH_KEY);
        config.setValue(passwordEncoder.encode(request.password()));
        appConfigRepository.save(config);

        String token = jwtUtil.generate();
        ResponseCookie cookie = cookieUtil.buildTokenCookie(token);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("message", "Vault configured successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody PasswordRequest request,
                                                     HttpServletResponse response) {
        Optional<AppConfig> passwordConfig = appConfigRepository.findById(PASSWORD_HASH_KEY);

        if (passwordConfig.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "App not configured"));
        }

        String hash = passwordConfig.get().getValue();
        if (request.password() != null && passwordEncoder.matches(request.password(), hash)) {
            String token = jwtUtil.generate();
            ResponseCookie cookie = cookieUtil.buildTokenCookie(token);
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return ResponseEntity.ok(Map.of("message", "Login successful"));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Wrong password"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        ResponseCookie cookie = cookieUtil.buildClearCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verify() {
        return ResponseEntity.ok(Map.of("valid", true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(HttpServletResponse response) {
        String token = jwtUtil.generate();
        ResponseCookie cookie = cookieUtil.buildTokenCookie(token);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Token refreshed"));
    }

    public record PasswordRequest(String password) {
    }
}