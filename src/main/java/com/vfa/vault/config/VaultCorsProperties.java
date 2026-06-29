package com.vfa.vault.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "vault")
public class VaultCorsProperties {

    static final String ALLOWED_METHODS =
            "GET,POST,PUT,PATCH,DELETE,OPTIONS";

    private List<String> allowedOrigins = new ArrayList<>();
    private String frontendUrl = "";

    private List<String> resolvedOrigins;

    @PostConstruct
    void init() {
        List<String> merged = new ArrayList<>(allowedOrigins);
        if (frontendUrl != null && !frontendUrl.isBlank() && !merged.contains(frontendUrl)) {
            merged.add(frontendUrl);
        }
        this.resolvedOrigins = List.copyOf(merged);
    }

    public List<String> getAllowedOrigins() {
        return resolvedOrigins;
    }

    public boolean isAllowedOrigin(String origin) {
        return origin != null && resolvedOrigins.contains(origin);
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins != null ? new ArrayList<>(allowedOrigins) : new ArrayList<>();
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }
}
