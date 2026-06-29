package com.vfa.vault.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vfa.vault.ai.LlmProviderRouter;
import com.vfa.vault.ai.ModelDiscoveryService;
import com.vfa.vault.dto.AiConfigResponseDTO;
import com.vfa.vault.dto.AiConfigUpdateDTO;
import com.vfa.vault.dto.ChatRequestDTO;
import com.vfa.vault.dto.ChatResponseDTO;
import com.vfa.vault.entity.LlmProviderConfig;
import com.vfa.vault.repository.LlmProviderConfigRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final LlmProviderRouter llmProviderRouter;
    private final LlmProviderConfigRepository configRepository;
    private final ModelDiscoveryService modelDiscoveryService;
    private final ChatMemory chatMemory;
    private final ObjectMapper objectMapper;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        ChatClient chatClient = llmProviderRouter.getClientForTask(LlmProviderRouter.TaskType.CHAT);
        LlmProviderConfig config = configRepository.getConfig();

        String reply;
        boolean hasConversation = request.conversationId() != null
                && !request.conversationId().isBlank();

        if (hasConversation) {
            reply = chatClient.prompt()
                    .user(request.message())
                    .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                            .conversationId(request.conversationId())
                            .build())
                    .call()
                    .content();
        } else {
            reply = chatClient.prompt()
                    .user(request.message())
                    .call()
                    .content();
        }

        return ResponseEntity.ok(new ChatResponseDTO(
                reply,
                config.getChatProvider(),
                config.getChatModel(),
                Collections.emptyList()
        ));
    }

    @GetMapping("/config")
    public ResponseEntity<AiConfigResponseDTO> getConfig() {
        LlmProviderConfig config = configRepository.getConfig();
        return ResponseEntity.ok(buildConfigResponse(config));
    }

    @PatchMapping("/config")
    public ResponseEntity<AiConfigResponseDTO> updateConfig(
            @Valid @RequestBody AiConfigUpdateDTO request) {
        if (!"chat".equals(request.task()) && !"summary".equals(request.task())) {
            throw new IllegalArgumentException("task must be 'chat' or 'summary'");
        }
        if (!"lmstudio".equals(request.provider()) && !"groq".equals(request.provider())) {
            throw new IllegalArgumentException("provider must be 'lmstudio' or 'groq'");
        }

        LlmProviderConfig config = configRepository.getConfig();

        if ("chat".equals(request.task())) {
            config.setChatProvider(request.provider());
            config.setChatModel(request.model());
        } else {
            config.setSummaryProvider(request.provider());
            config.setSummaryModel(request.model());
        }
        config.setUpdatedAt(LocalDateTime.now());
        config = configRepository.save(config);

        return ResponseEntity.ok(buildConfigResponse(config));
    }

    @GetMapping("/models/lmstudio")
    public ResponseEntity<List<String>> getLmStudioModels() {
        return ResponseEntity.ok(modelDiscoveryService.getLmStudioModels());
    }

    @GetMapping("/models/groq")
    public ResponseEntity<List<String>> getGroqModels() {
        return ResponseEntity.ok(modelDiscoveryService.getGroqModels());
    }

    private AiConfigResponseDTO buildConfigResponse(LlmProviderConfig config) {
        List<String> lmStudioModels = parseJsonList(config.getLmstudioModels());
        List<String> groqModels = parseJsonList(config.getGroqModels());

        return new AiConfigResponseDTO(
                new AiConfigResponseDTO.AiTaskConfig(config.getChatProvider(), config.getChatModel()),
                new AiConfigResponseDTO.AiTaskConfig(config.getSummaryProvider(), config.getSummaryModel()),
                Map.of("lmstudio", lmStudioModels, "groq", groqModels)
        );
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON model list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
