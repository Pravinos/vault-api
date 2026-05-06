package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponseDTO(
        UUID id,
        String fromAccountName,
        String toAccountName,
        BigDecimal amount,
        String note,
        LocalDate transferDate,
        LocalDateTime createdAt
) {}
