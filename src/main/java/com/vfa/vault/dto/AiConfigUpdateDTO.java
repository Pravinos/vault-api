package com.vfa.vault.dto;

import jakarta.validation.constraints.NotBlank;

public record AiConfigUpdateDTO(
        @NotBlank(message = "task is required") String task,
        @NotBlank(message = "provider is required") String provider,
        @NotBlank(message = "model is required") String model
) {
}
