package com.vfa.vault.dto;

import java.math.BigDecimal;

public record BudgetSummaryDTO(
        Integer categoryId,
        String categoryName,
        String categoryIcon,
        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        double percentageUsed,
        BudgetStatus status
) {}
