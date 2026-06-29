package com.vfa.vault.ai;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vfa.vault.entity.LlmProviderConfig;
import com.vfa.vault.repository.LlmProviderConfigRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ModelDiscoveryService {

    private final LlmProviderConfigRepository configRepo;
    private final ObjectMapper objectMapper;
    private final String groqApiKey;

    public ModelDiscoveryService(
            LlmProviderConfigRepository configRepo,
            ObjectMapper objectMapper,
            @Value("${GROQ_API_KEY:}") String groqApiKey) {
        this.configRepo = configRepo;
        this.objectMapper = objectMapper;
        this.groqApiKey = groqApiKey;
    }

    public List<String> getLmStudioModels() {
        try {
            RestClient client = RestClient.create();
            String json = client.get()
                    .uri("http://localhost:1234/v1/models")
                    .retrieve()
                    .body(String.class);
            List<String> models = parseModelIds(json);
            updateCachedModels("lmstudio", models);
            return models;
        } catch (RestClientException e) {
            log.warn("LM Studio unreachable, returning cached models: {}", e.getMessage());
            return getCachedModels("lmstudio");
        }
    }

    public List<String> getGroqModels() {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("GROQ_API_KEY not configured, returning cached models");
            return getCachedModels("groq");
        }
        try {
            RestClient client = RestClient.create();
            String json = client.get()
                    .uri("https://api.groq.com/openai/v1/models")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .retrieve()
                    .body(String.class);
            List<String> models = parseModelIds(json).stream()
                    .filter(id -> !id.contains("whisper") && !id.contains("vision-preview"))
                    .sorted()
                    .toList();
            updateCachedModels("groq", models);
            return models;
        } catch (RestClientException e) {
            log.warn("Groq API unreachable, returning cached models: {}", e.getMessage());
            return getCachedModels("groq");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseModelIds(String json) {
        try {
            Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
            Object data = response.get("data");
            if (data instanceof List<?> list) {
                return list.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> (String) ((Map<?, ?>) item).get("id"))
                        .filter(id -> id != null)
                        .sorted()
                        .toList();
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse models response", e);
        }
        return new ArrayList<>();
    }

    private void updateCachedModels(String provider, List<String> models) {
        try {
            String json = objectMapper.writeValueAsString(models);
            LlmProviderConfig config = configRepo.getConfig();
            if ("lmstudio".equals(provider)) {
                config.setLmstudioModels(json);
            } else {
                config.setGroqModels(json);
            }
            config.setUpdatedAt(LocalDateTime.now());
            configRepo.save(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize models list", e);
        }
    }

    private List<String> getCachedModels(String provider) {
        return configRepo.findConfig().map(config -> {
            String json = "lmstudio".equals(provider)
                    ? config.getLmstudioModels()
                    : config.getGroqModels();
            if (json == null || json.isBlank()) return new ArrayList<String>();
            try {
                return objectMapper.<List<String>>readValue(json, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to parse cached models", e);
                return new ArrayList<String>();
            }
        }).orElse(new ArrayList<>());
    }
}
