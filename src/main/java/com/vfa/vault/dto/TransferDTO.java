package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferDTO(
        @NotNull(message = "Source account is required")
        UUID fromAccountId,
        @NotNull(message = "Destination account is required")
        UUID toAccountId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,
        @Size(max = 255, message = "Note must be under 255 characters")
        String note,
        LocalDate transferDate
) {}
