package com.vfa.vault.dto;

import com.vfa.vault.entity.Goal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

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

            LocalDate deadline
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
            long daysRemaining
    ) {}

    public record ContributeRequest(
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            BigDecimal amount
    ) {}
}
