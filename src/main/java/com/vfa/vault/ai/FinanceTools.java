package com.vfa.vault.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.vfa.vault.dto.DashboardResponseDTO;
import com.vfa.vault.entity.Income;
import com.vfa.vault.repository.ExpenseRepository;
import com.vfa.vault.repository.IncomeRepository;
import com.vfa.vault.service.DashboardService;
import com.vfa.vault.service.GoalService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FinanceTools {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
        private final DashboardService dashboardService;
    private final GoalService goalService;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public record BudgetStatus(
            String currentMonth,
            double currentMonthTotal,
            String previousMonth,
            double previousMonthTotal,
            double changeAmount,
            double changePercentage
    ) {}

    

    public record DailySpend(String date, double total) {}

    public record AccountSummary(
            String name,
            String accountType,
            double calculatedBalance,
            Double manualBalance,
            Double contributedAmount,
            Double currentValue,
            Double returnAmount,
            Double returnPercentage,
            String platform,
            String instrument
    ) {}

        @Tool(description = "Get the current dashboard summary including net worth, monthly income, expenses, and account balances")
        public DashboardResponseDTO getDashboardSummary() {
                return dashboardService.getDashboard();
        }

    @Tool(description = "Get total expenses grouped by category for a given month. Month format: YYYY-MM")
    public Map<String, Double> getExpensesByCategory(String month) {
        return expenseRepository.sumByCategoryForMonth(month).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((BigDecimal) row[1]).doubleValue()
                ));
    }

    @Tool(description = "Get the current month's total spending and how it compares to the previous month")
    public BudgetStatus getBudgetStatus() {
        DashboardResponseDTO dashboard = dashboardService.getDashboard();
        YearMonth now = YearMonth.now();
        YearMonth prev = now.minusMonths(1);
        String thisMonth = now.format(MONTH_FMT);
        String lastMonth = prev.format(MONTH_FMT);

        BigDecimal thisTotal = dashboard.getExpensesThisMonth() != null
                ? dashboard.getExpensesThisMonth()
                : BigDecimal.ZERO;
        BigDecimal lastTotal = dashboard.getExpensesLastMonth() != null
                ? dashboard.getExpensesLastMonth()
                : BigDecimal.ZERO;

        double change = thisTotal.subtract(lastTotal).doubleValue();
        double changePct = lastTotal.compareTo(BigDecimal.ZERO) != 0
                ? Math.round((change / lastTotal.doubleValue()) * 10000.0) / 100.0
                : 0.0;

        return new BudgetStatus(thisMonth, thisTotal.doubleValue(),
                lastMonth, lastTotal.doubleValue(), change, changePct);
    }

    @Tool(description = "Get progress for all active goals: name, target, live saved amount derived from linked account balances, percentage complete, days remaining, and whether the goal is overdue.")
    public List<GoalProgress> getGoalProgress() {
        return goalService.findAllActive().stream()
                .map(g -> new GoalProgress(
                        g.name(),
                        g.targetAmount() != null ? g.targetAmount().doubleValue() : 0.0,
                        g.savedAmount() != null ? g.savedAmount().doubleValue() : 0.0,
                        g.progressPercentage(),
                        g.daysRemaining(),
                        g.isOverdue(),
                        g.linkedAccounts() != null
                                ? g.linkedAccounts().stream()
                                        .map(la -> new LinkedAccountSummary(
                                                la.id(),
                                                la.name(),
                                                la.accountType(),
                                                la.calculatedBalance()))
                                        .toList()
                                : List.<LinkedAccountSummary>of()))
                .toList();
    }

    public record LinkedAccountSummary(UUID id, String name, String accountType, BigDecimal calculatedBalance) {}

    public record GoalProgress(
            String name,
            double targetAmount,
            double savedAmount,
            double progressPercentage,
            long daysRemaining,
            boolean isOverdue,
            List<LinkedAccountSummary> linkedAccounts
    ) {}

    @Tool(description = "Get daily spending totals for the last N days. Useful for trend questions.")
    public List<DailySpend> getDailySpending(int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        return expenseRepository.dailyTotalsFrom(since).stream()
                .map(row -> new DailySpend(
                        (String) row[0],
                        ((BigDecimal) row[1]).doubleValue()))
                .toList();
    }

    @Tool(description = "Get total spending for a specific category over the last N months")
    public Map<String, Double> getCategoryTrend(String category, int months) {
        LocalDate since = LocalDate.now().minusMonths(months).withDayOfMonth(1);
        return expenseRepository.monthlyTotalsByCategory(category, since).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((BigDecimal) row[1]).doubleValue()
                ));
    }

    @Tool(description = "Get all accounts with their calculated and manual balances. For investment accounts, includes return amount and percentage.")
    public List<AccountSummary> getAccountSummaries() {
        DashboardResponseDTO dashboard = dashboardService.getDashboard();
        List<com.vfa.vault.dto.AccountDashboardDTO> accounts =
                dashboard.getAccounts() != null ? dashboard.getAccounts() : Collections.emptyList();

        return accounts.stream()
                .map(a -> new AccountSummary(
                        a.getName(),
                        a.getAccountType(),
                        valueOrZero(a.getCalculatedBalance()),
                        toDoubleOrNull(a.getManualBalance()),
                        "INVESTMENT".equals(a.getAccountType()) ? valueOrZero(a.getCalculatedBalance()) : null,
                        toDoubleOrNull(a.getCurrentValue()),
                        toDoubleOrNull(a.getReturnAmount()),
                        toDoubleOrNull(a.getReturnPercentage()),
                        null,
                        null))
                .toList();
    }

    @Tool(description = "Get total income grouped by category for a given month. Month format: YYYY-MM")
    public Map<String, Double> getIncomeByCategory(String month) {
        return incomeRepository.sumByCategoryForMonth(month).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((BigDecimal) row[1]).doubleValue()
                ));
    }

    @Tool(description = "Get net cash flow (income minus expenses) for a given month. Month format: YYYY-MM")
    public Double getNetCashFlow(String month) {
        if (month == null || isCurrentMonth(month)) {
            DashboardResponseDTO dashboard = dashboardService.getDashboard();
            return valueOrZero(dashboard.getNetCashFlow());
        }

        BigDecimal totalIncome = incomeRepository.findByFilters(month, null).stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenseRepository.totalForMonth(month);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        return totalIncome.subtract(totalExpenses).doubleValue();
    }

        private boolean isCurrentMonth(String month) {
                try {
                        return YearMonth.parse(month, MONTH_FMT).equals(YearMonth.now());
                } catch (DateTimeParseException ex) {
                        return false;
                }
        }

        private Double toDoubleOrNull(BigDecimal value) {
                return value != null ? value.doubleValue() : null;
        }

        private double valueOrZero(BigDecimal value) {
                return value != null ? value.doubleValue() : 0.0;
        }
}
