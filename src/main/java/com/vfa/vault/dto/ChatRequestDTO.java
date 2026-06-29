package com.vfa.vault.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        @NotBlank(message = "message is required")
        String message,
        String conversationId
) {
}
