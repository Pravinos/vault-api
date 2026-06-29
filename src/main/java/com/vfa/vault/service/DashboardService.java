package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.AccountDashboardDTO;
import com.vfa.vault.dto.BudgetSummaryDTO;
import com.vfa.vault.dto.DashboardResponseDTO;
import com.vfa.vault.entity.Account;
import com.vfa.vault.entity.AccountType;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.ExpenseRepository;
import com.vfa.vault.repository.IncomeRepository;
import com.vfa.vault.service.AccountBalanceService.BalanceBreakdown;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final AccountRepository accountRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetService budgetService;
    private final AccountBalanceService accountBalanceService;
    private final InvestmentBalanceService investmentBalanceService;

    @Transactional(readOnly = true)
    public DashboardResponseDTO getDashboard() {
        YearMonth current = YearMonth.now();
        YearMonth last = current.minusMonths(1);
        int daysElapsed = LocalDate.now().getDayOfMonth();

        List<Account> accounts = accountRepository.findAllOrderByLastUpdatedDesc();
        Map<UUID, BalanceBreakdown> breakdowns = accountBalanceService.getBreakdowns(accounts);
        List<UUID> investmentIds = accounts.stream()
                .filter(account -> account.getAccountType() == AccountType.INVESTMENT)
                .map(Account::getId)
                .toList();
        Map<UUID, BigDecimal> checkpointValues =
                investmentBalanceService.loadLatestCheckpointValues(investmentIds);

        List<AccountDashboardDTO> accountDTOs = accounts.stream()
                .map(account -> buildAccountDTO(
                        account,
                        breakdowns.get(account.getId()),
                        checkpointValues))
                .toList();

        BigDecimal calculatedNetWorth = accountDTOs.stream()
                .map(AccountDashboardDTO::getCalculatedBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasManual = accountDTOs.stream().anyMatch(a -> a.getManualBalance() != null);
        BigDecimal manualNetWorth = hasManual
                ? accountDTOs.stream()
                        .filter(a -> a.getManualBalance() != null)
                        .map(AccountDashboardDTO::getManualBalance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;

        BigDecimal drift = null;
        if (manualNetWorth != null) {
            BigDecimal referenceForManualAccounts = accountDTOs.stream()
                    .filter(dto -> dto.getManualBalance() != null)
                    .map(this::manualDriftReference)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            drift = referenceForManualAccounts.subtract(manualNetWorth);
        }

        BigDecimal incomeThis = incomeRepository
                .sumByYearMonth(current.atDay(1), current.atEndOfMonth())
                .orElse(BigDecimal.ZERO);
        BigDecimal incomeLast = incomeRepository
                .sumByYearMonth(last.atDay(1), last.atEndOfMonth())
                .orElse(BigDecimal.ZERO);

        BigDecimal expensesThis = expenseRepository
                .sumByYearMonth(current.atDay(1), current.atEndOfMonth())
                .orElse(BigDecimal.ZERO);
        BigDecimal expensesLast = expenseRepository
                .sumByYearMonth(last.atDay(1), last.atEndOfMonth())
                .orElse(BigDecimal.ZERO);

        BigDecimal netCashFlow = incomeThis.subtract(expensesThis);

        BigDecimal dailyAvg = daysElapsed > 0
                ? expensesThis.divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        var topCat = expenseRepository
                .findTopCategoryForDateRange(current.atDay(1), current.atEndOfMonth());

        String topCategoryName = "N/A";
        BigDecimal topCategoryAmount = BigDecimal.ZERO;
        if (topCat.isPresent()) {
            topCategoryName = topCat.get().getCategoryName();
            topCategoryAmount = nvl(topCat.get().getTotal());
        }

        BigDecimal expenseMoM = calcMoMPercent(expensesThis, expensesLast);
        BigDecimal incomeMoM = calcMoMPercent(incomeThis, incomeLast);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        String currentLabel = current.atDay(1).format(fmt);
        String lastLabel = last.atDay(1).format(fmt);

        String currentMonth = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<BudgetSummaryDTO> budgetAlerts = budgetService.getBudgetAlerts(currentMonth);

        log.info("Dashboard computed: accounts={}, netWorth={}, manualNetWorth={}, drift={}, incomeThis={}, expensesThis={}, topCategory={} ({})",
                accountDTOs.size(), calculatedNetWorth, manualNetWorth, drift, incomeThis, expensesThis, topCategoryName, topCategoryAmount);

        return DashboardResponseDTO.builder()
                .calculatedNetWorth(calculatedNetWorth)
                .manualNetWorth(manualNetWorth)
                .netWorthDrift(drift)
                .accounts(accountDTOs)
                .incomeThisMonth(incomeThis)
                .expensesThisMonth(expensesThis)
                .netCashFlow(netCashFlow)
                .incomeLastMonth(incomeLast)
                .expensesLastMonth(expensesLast)
                .dailyAverageExpense(dailyAvg)
                .topExpenseCategory(topCategoryName)
                .topExpenseCategoryAmount(topCategoryAmount)
                .currentMonthLabel(currentLabel)
                .lastMonthLabel(lastLabel)
                .expensesMoMPercent(expenseMoM)
                .incomeMoMPercent(incomeMoM)
                .budgetAlerts(budgetAlerts)
                .build();
    }

    private AccountDashboardDTO buildAccountDTO(
            Account account,
            BalanceBreakdown breakdown,
            Map<UUID, BigDecimal> checkpointValues) {
        BigDecimal opening = nvl(breakdown.openingBalance());
        BigDecimal contributedBalance = breakdown.calculatedBalance();

        log.debug("Dashboard account breakdown id={}, name={}, opening={}, income={}, expenses={}, transfersIn={}, transfersOut={}, calculated={}",
                account.getId(),
                account.getName(),
                opening,
                breakdown.totalIncome(),
                breakdown.totalExpenses(),
                breakdown.incomingTransfers(),
                breakdown.outgoingTransfers(),
                contributedBalance);

        BigDecimal displayBalance = contributedBalance;
        BigDecimal sinceOpening = contributedBalance.subtract(opening);

        BigDecimal currentValue = null;
        BigDecimal returnAmount = null;
        BigDecimal returnPct = null;
        AccountType accountType = account.getAccountType();

        if (accountType == AccountType.INVESTMENT) {
            currentValue = investmentBalanceService.resolveCurrentValue(
                    account, contributedBalance, checkpointValues);
            displayBalance = currentValue;
            sinceOpening = displayBalance.subtract(opening);
            returnAmount = investmentBalanceService.computeReturnAmount(contributedBalance, currentValue);
            returnPct = investmentBalanceService.computeReturnPercentageForDisplay(
                    contributedBalance, currentValue);
        }

        String secondaryLabel;
        boolean secondaryPositive;
        if (accountType == AccountType.INVESTMENT && returnPct != null) {
            secondaryPositive = returnPct.compareTo(BigDecimal.ZERO) >= 0;
            secondaryLabel = (secondaryPositive ? "+" : "") + returnPct + "% return";
        } else {
            secondaryPositive = sinceOpening.compareTo(BigDecimal.ZERO) >= 0;
            secondaryLabel = formatAmount(sinceOpening) + " since opening";
        }

        return AccountDashboardDTO.builder()
                .id(account.getId())
                .name(account.getName())
                .accountType(accountType != null ? accountType.name() : "UNKNOWN")
                .calculatedBalance(displayBalance)
                .manualBalance(account.getManualBalance())
                .openingBalance(opening)
                .sinceOpening(sinceOpening)
                .currentValue(currentValue)
                .returnAmount(returnAmount)
                .returnPercentage(returnPct)
                .secondaryLabel(secondaryLabel)
                .secondaryPositive(secondaryPositive)
                .build();
    }

    private BigDecimal calcMoMPercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private BigDecimal manualDriftReference(AccountDashboardDTO account) {
        if ("INVESTMENT".equals(account.getAccountType()) && account.getCurrentValue() != null) {
            return account.getCurrentValue();
        }
        return account.getCalculatedBalance();
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        return (scaled.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + scaled;
    }
}
