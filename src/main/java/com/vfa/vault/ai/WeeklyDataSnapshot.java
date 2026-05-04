package com.vfa.vault.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeeklyDataSnapshot(
        LocalDate weekStart,
        LocalDate weekEnd,
        BigDecimal totalSpent,
        BigDecimal totalIncome,
        BigDecimal netCashFlow,
        Map<String, BigDecimal> spendingByCategory,
        Map<String, BigDecimal> incomeByCategory,
        List<GoalSnapshotItem> goals,
        List<AccountSnapshotItem> accounts
) {
    public record GoalSnapshotItem(
            String name,
            BigDecimal target,
            BigDecimal saved,
            BigDecimal percentage,
            Long daysRemaining
    ) {}

    public record AccountSnapshotItem(
            String name,
            String type,
            BigDecimal calculatedBalance,
            BigDecimal manualBalance,
            BigDecimal contributedAmount,
            BigDecimal currentValue,
            BigDecimal returnPercentage
    ) {}
}
