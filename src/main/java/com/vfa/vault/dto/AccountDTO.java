package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.vfa.vault.entity.AccountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AccountDTO {

    public record Request(
            @NotBlank(message = "Account name is required")
            @Size(max = 100, message = "Account name must be under 100 characters")
            String name,
            @NotNull(message = "Account type is required")
            AccountType accountType,
            @NotNull(message = "Opening balance is required")
            @DecimalMin(value = "0.00", message = "Opening balance must be >= 0")
            BigDecimal openingBalance,
            String platform,
            String instrument,
            String assetType
    ) {}

    public record Response(
            UUID id,
            String name,
            AccountType accountType,
            BigDecimal openingBalance,
            BigDecimal manualBalance,
            LocalDateTime manualBalanceUpdatedAt,
            LocalDateTime createdAt,
            BigDecimal calculatedBalance,
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            // Investment-only fields (null for non-investment accounts)
            String platform,
            String instrument,
            String assetType,
            BigDecimal contributedAmount,
            BigDecimal currentValue,
            BigDecimal returnAmount,
            BigDecimal returnPercentage
    ) {}

    public record ManualBalanceRequest(
            @NotNull(message = "Manual balance is required")
            @DecimalMin(value = "0.00", message = "Manual balance must be >= 0")
            BigDecimal manualBalance,
            Boolean alsoSetAsOpeningBalance
    ) {}
}
