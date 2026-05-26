package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.vfa.vault.entity.Goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class GoalDTO {

    public record Request(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must be under 100 characters")
            String name,

            @Size(max = 255, message = "Description must be under 255 characters")
            String description,

            @NotNull(message = "Target amount is required")
            @DecimalMin(value = "0.01", message = "Target amount must be greater than 0")
            BigDecimal targetAmount,

            @NotNull(message = "Goal type is required")
            Goal.GoalType goalType,

            LocalDate deadline,

            Set<UUID> accountIds
    ) {}

    public record Response(
            UUID id,
            String name,
            String description,
            BigDecimal targetAmount,
            BigDecimal savedAmount,
            Goal.GoalType goalType,
            LocalDate deadline,
            LocalDateTime createdAt,
            Boolean isActive,
            double progressPercentage,
            long daysRemaining,
            boolean isOverdue,
            List<LinkedAccountSummary> linkedAccounts
    ) {}

    public record LinkedAccountSummary(
            UUID id,
            String name,
            String accountType,
            BigDecimal calculatedBalance
    ) {}

        // ContributeRequest removed: contributions are derived from linked account balances now.
}
