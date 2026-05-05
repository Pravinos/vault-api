package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDTO {

    private BigDecimal calculatedNetWorth;
    private BigDecimal manualNetWorth;
    private BigDecimal netWorthDrift;

    private List<AccountDashboardDTO> accounts;

    private BigDecimal incomeThisMonth;
    private BigDecimal expensesThisMonth;
    private BigDecimal netCashFlow;

    private BigDecimal incomeLastMonth;
    private BigDecimal expensesLastMonth;

    private BigDecimal dailyAverageExpense;
    private String topExpenseCategory;
    private BigDecimal topExpenseCategoryAmount;

    private String currentMonthLabel;
    private String lastMonthLabel;

    private BigDecimal expensesMoMPercent;
    private BigDecimal incomeMoMPercent;
}
