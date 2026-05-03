package com.vfa.vault.dto;

import java.util.List;
import java.util.Map;

public record AiConfigResponseDTO(
        AiTaskConfig chat,
        AiTaskConfig summary,
        Map<String, List<String>> availableModels
) {
    public record AiTaskConfig(String provider, String model) {
    }
}
