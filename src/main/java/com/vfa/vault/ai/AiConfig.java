package com.vfa.vault.ai;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.lmstudio.base-url}")
    private String lmStudioBaseUrl;

    @Value("${spring.ai.openai.lmstudio.api-key}")
    private String lmStudioApiKey;

    @Value("${spring.ai.openai.lmstudio.chat.model}")
    private String lmStudioChatModel;

    @Value("${spring.ai.openai.groq.base-url}")
    private String groqBaseUrl;

    @Value("${spring.ai.openai.groq.api-key:}")
    private String groqApiKey;

    @Value("${spring.ai.openai.groq.chat.model}")
    private String groqChatModel;

    @Value("${vault.ai.system-prompt}")
    private String systemPromptValue;

    @Bean
    @Qualifier("lmStudioModel")
    public OpenAiChatModel lmStudioModel() {
        return OpenAiChatModel.builder()
            .options(
                        OpenAiChatOptions.builder()
                                .baseUrl(lmStudioBaseUrl)
                                .apiKey(lmStudioApiKey)
                                .model(lmStudioChatModel)
                                .temperature(0.3)
                                .build())
                .build();
    }

    @Bean
    @Qualifier("groqModel")
    public OpenAiChatModel groqModel() {
        String key = (groqApiKey != null && !groqApiKey.isBlank()) ? groqApiKey : "placeholder";
        return OpenAiChatModel.builder()
            .options(
                        OpenAiChatOptions.builder()
                                .baseUrl(groqBaseUrl)
                                .apiKey(key)
                                .model(groqChatModel)
                                .temperature(0.3)
                                .build())
                .build();
    }

    @Bean("aiSystemPrompt")
    public String aiSystemPrompt() {
        return systemPromptValue;
    }

    @Bean
    public InMemoryChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public ChatMemory chatMemory(InMemoryChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }
}
