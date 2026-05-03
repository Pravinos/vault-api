package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class InvestmentCheckpointDTO {

    public record Request(
            @NotNull(message = "Value is required")
            @DecimalMin(value = "0.01", message = "Value must be greater than 0")
            BigDecimal value,
            String note
    ) {}

    public record Response(
            UUID id,
            BigDecimal value,
            LocalDateTime recordedAt,
            String note
    ) {}
}
