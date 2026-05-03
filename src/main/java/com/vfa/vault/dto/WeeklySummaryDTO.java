package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class WeeklySummaryDTO {

    public record Response(
            UUID id,
            LocalDate weekStart,
            LocalDate weekEnd,
            String summaryText,
            BigDecimal totalSpent,
            LocalDateTime generatedAt,
            String provider
    ) {}
}
