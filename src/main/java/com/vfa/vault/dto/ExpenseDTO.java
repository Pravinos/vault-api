package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ExpenseDTO {

    public record Request(
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            BigDecimal amount,
            @Size(max = 255, message = "Note must be under 255 characters")
            String note,
            @NotNull(message = "Category is required")
            Integer categoryId,
            LocalDate expenseDate,
            @NotNull(message = "Account is required")
            UUID accountId
            ) {

    }

    public record Response(
            UUID id,
            BigDecimal amount,
            String note,
            CategoryDTO.Response category,
            LocalDate expenseDate,
            LocalDateTime createdAt,
            UUID accountId,
            String accountName
            ) {

    }

    public record MonthlySummary(
            String month,
            BigDecimal total,
            java.util.List<CategoryTotal> byCategory
            ) {

    }

    public record CategoryTotal(
            String category,
            BigDecimal total
            ) {

    }

    public record Stats(
            BigDecimal totalThisMonth,
            BigDecimal totalLastMonth,
            BigDecimal averagePerDay,
            String topCategory,
            long totalExpensesThisMonth
            ) {

    }
}
