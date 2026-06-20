package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BudgetDTO {

    public record Request(
            @NotNull(message = "Category id is required")
            Integer categoryId,
            @NotBlank(message = "Month is required")
            String month,
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            BigDecimal amount
    ) {}

    public record Response(
            UUID id,
            Integer categoryId,
            String categoryName,
            String categoryIcon,
            String month,
            BigDecimal amount
    ) {}
}
