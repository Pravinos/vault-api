package com.vfa.vault.dto;

import java.util.List;

public record ChatResponseDTO(
        String reply,
        String provider,
        String model,
        List<String> functionCallsUsed
) {
}
