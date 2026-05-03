package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IncomeDTO {

    public record Request(
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            BigDecimal amount,
            @Size(max = 255, message = "Note must be under 255 characters")
            String note,
            @NotNull(message = "Income category is required")
            Integer incomeCategoryId,
            @NotNull(message = "Account is required")
            UUID accountId,
            @NotNull(message = "Income date is required")
            LocalDate incomeDate
    ) {}

    public record Response(
            UUID id,
            BigDecimal amount,
            String note,
            Integer incomeCategoryId,
            UUID accountId,
            LocalDate incomeDate,
            LocalDateTime createdAt,
            String categoryName,
            String categoryIcon,
            String accountName
    ) {}
}
