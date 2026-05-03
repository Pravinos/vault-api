package com.vfa.vault.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.vfa.vault.entity.Income;
import com.vfa.vault.repository.ExpenseRepository;
import com.vfa.vault.repository.IncomeRepository;
import com.vfa.vault.service.AccountService;
import com.vfa.vault.service.GoalService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FinanceTools {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final AccountService accountService;
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

    public record GoalProgress(
            String name,
            double targetAmount,
            double savedAmount,
            double progressPercentage,
            long daysRemaining
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
        YearMonth now = YearMonth.now();
        YearMonth prev = now.minusMonths(1);
        String thisMonth = now.format(MONTH_FMT);
        String lastMonth = prev.format(MONTH_FMT);

        BigDecimal thisTotal = expenseRepository.totalForMonth(thisMonth);
        if (thisTotal == null) thisTotal = BigDecimal.ZERO;
        BigDecimal lastTotal = expenseRepository.totalForMonth(lastMonth);
        if (lastTotal == null) lastTotal = BigDecimal.ZERO;

        double change = thisTotal.subtract(lastTotal).doubleValue();
        double changePct = lastTotal.compareTo(BigDecimal.ZERO) != 0
                ? Math.round((change / lastTotal.doubleValue()) * 10000.0) / 100.0
                : 0.0;

        return new BudgetStatus(thisMonth, thisTotal.doubleValue(),
                lastMonth, lastTotal.doubleValue(), change, changePct);
    }

    @Tool(description = "Get progress for all active goals: name, target, saved, percentage, and days remaining")
    public List<GoalProgress> getGoalProgress() {
        return goalService.findAllActive().stream()
                .map(g -> new GoalProgress(
                        g.name(),
                        g.targetAmount().doubleValue(),
                        g.savedAmount().doubleValue(),
                        g.progressPercentage(),
                        g.daysRemaining()))
                .toList();
    }

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
        return accountService.getAllAccounts().stream()
                .map(a -> new AccountSummary(
                        a.name(),
                        a.accountType().name(),
                        a.calculatedBalance().doubleValue(),
                        a.manualBalance() != null ? a.manualBalance().doubleValue() : null,
                        a.contributedAmount() != null ? a.contributedAmount().doubleValue() : null,
                        a.currentValue() != null ? a.currentValue().doubleValue() : null,
                        a.returnAmount() != null ? a.returnAmount().doubleValue() : null,
                        a.returnPercentage() != null ? a.returnPercentage().doubleValue() : null,
                        a.platform(),
                        a.instrument()))
                .toList();
    }

    @Tool(description = "Get total income grouped by category for a given month. Month format: YYYY-MM")
    public Map<String, Double> getIncomeByCategory(String month) {
        return incomeRepository.findByFilters(month, null).stream()
                .collect(Collectors.groupingBy(
                        i -> i.getIncomeCategory().getName(),
                        Collectors.collectingAndThen(
                                Collectors.reducing(BigDecimal.ZERO, Income::getAmount, BigDecimal::add),
                                BigDecimal::doubleValue)));
    }

    @Tool(description = "Get net cash flow (income minus expenses) for a given month. Month format: YYYY-MM")
    public Double getNetCashFlow(String month) {
        if (month == null) month = YearMonth.now().format(MONTH_FMT);
        BigDecimal totalIncome = incomeRepository.findByFilters(month, null).stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenseRepository.totalForMonth(month);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        return totalIncome.subtract(totalExpenses).doubleValue();
    }
}
