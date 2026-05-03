package com.vfa.vault.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.vfa.vault.entity.LlmProviderConfig;
import com.vfa.vault.repository.LlmProviderConfigRepository;

@Service
public class LlmProviderRouter {

    public enum TaskType { CHAT, SUMMARY }

    private final OpenAiChatModel lmStudioModel;
    private final OpenAiChatModel groqModel;
    private final LlmProviderConfigRepository configRepo;
    private final FinanceTools financeTools;
    private final String systemPrompt;

    public LlmProviderRouter(
            @Qualifier("lmStudioModel") OpenAiChatModel lmStudioModel,
            @Qualifier("groqModel") OpenAiChatModel groqModel,
            LlmProviderConfigRepository configRepo,
            FinanceTools financeTools,
            @Qualifier("aiSystemPrompt") String systemPrompt) {
        this.lmStudioModel = lmStudioModel;
        this.groqModel = groqModel;
        this.configRepo = configRepo;
        this.financeTools = financeTools;
        this.systemPrompt = systemPrompt;
    }

    public ChatClient getClientForTask(TaskType task) {
        LlmProviderConfig config = configRepo.findById(1)
                .orElseThrow(() -> new IllegalStateException("LLM provider config not found (id=1)"));

        String provider = task == TaskType.SUMMARY
                ? config.getSummaryProvider()
                : config.getChatProvider();

        String model = task == TaskType.SUMMARY
                ? config.getSummaryModel()
                : config.getChatModel();

        OpenAiChatModel baseModel = "groq".equals(provider) ? groqModel : lmStudioModel;

        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.3);

        ChatClient.Builder builder = ChatClient.builder(baseModel)
                .defaultSystem(systemPrompt)
                .defaultOptions(options);

        if (task == TaskType.CHAT) {
            builder.defaultTools(financeTools);
        }

        return builder.build();
    }
}
