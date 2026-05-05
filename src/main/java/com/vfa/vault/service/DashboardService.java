package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.AccountDashboardDTO;
import com.vfa.vault.dto.DashboardResponseDTO;
import com.vfa.vault.entity.Account;
import com.vfa.vault.entity.AccountType;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.ExpenseRepository;
import com.vfa.vault.repository.IncomeRepository;
import com.vfa.vault.repository.InvestmentCheckpointRepository;
import com.vfa.vault.repository.TransferRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

        private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final AccountRepository accountRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final TransferRepository transferRepository;
    private final InvestmentCheckpointRepository checkpointRepository;

    @Transactional(readOnly = true)
    public DashboardResponseDTO getDashboard() {
        YearMonth current = YearMonth.now();
        YearMonth last = current.minusMonths(1);
        int daysElapsed = LocalDate.now().getDayOfMonth();

        List<Account> accounts = accountRepository.findAllOrderByLastUpdatedDesc();
        List<AccountDashboardDTO> accountDTOs = accounts.stream()
                .map(this::buildAccountDTO)
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
                .sumByYearMonth(current.getYear(), current.getMonthValue())
                .orElse(BigDecimal.ZERO);
        BigDecimal incomeLast = incomeRepository
                .sumByYearMonth(last.getYear(), last.getMonthValue())
                .orElse(BigDecimal.ZERO);

        BigDecimal expensesThis = expenseRepository
                .sumByYearMonth(current.getYear(), current.getMonthValue())
                .orElse(BigDecimal.ZERO);
        BigDecimal expensesLast = expenseRepository
                .sumByYearMonth(last.getYear(), last.getMonthValue())
                .orElse(BigDecimal.ZERO);

        BigDecimal netCashFlow = incomeThis.subtract(expensesThis);

        BigDecimal dailyAvg = daysElapsed > 0
                ? expensesThis.divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Optional<Object[]> topCatRaw = expenseRepository
                .findTopCategoryByYearMonth(current.getYear(), current.getMonthValue());
        Object[] topCat = unwrapRow(topCatRaw.orElse(null));

        String topCategoryName = "N/A";
        BigDecimal topCategoryAmount = BigDecimal.ZERO;
        if (topCat != null) {
            if (topCat.length > 0 && topCat[0] != null) {
                topCategoryName = String.valueOf(topCat[0]);
            }
            if (topCat.length > 1 && topCat[1] != null) {
                topCategoryAmount = toBigDecimal(topCat[1]);
            }
        }

        BigDecimal expenseMoM = calcMoMPercent(expensesThis, expensesLast);
        BigDecimal incomeMoM = calcMoMPercent(incomeThis, incomeLast);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        String currentLabel = current.atDay(1).format(fmt);
        String lastLabel = last.atDay(1).format(fmt);

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
                .build();
    }

    private AccountDashboardDTO buildAccountDTO(Account account) {
        BigDecimal opening = nvl(account.getOpeningBalance());

        BigDecimal totalIncome = nvl(incomeRepository.sumByAccountId(account.getId()));
        BigDecimal totalExpenses = nvl(expenseRepository.sumByAccountId(account.getId()));
        BigDecimal transfersIn = transferRepository.sumByToAccountId(account.getId()).orElse(BigDecimal.ZERO);
        BigDecimal transfersOut = transferRepository.sumByFromAccountId(account.getId()).orElse(BigDecimal.ZERO);

        BigDecimal contributedBalance = opening
                .add(totalIncome)
                .subtract(totalExpenses)
                .add(transfersIn)
                .subtract(transfersOut);

        log.debug("Dashboard account breakdown id={}, name={}, opening={}, income={}, expenses={}, transfersIn={}, transfersOut={}, calculated={}",
                account.getId(),
                account.getName(),
                opening,
                totalIncome,
                totalExpenses,
                transfersIn,
                transfersOut,
                contributedBalance);

        BigDecimal displayBalance = contributedBalance;
        BigDecimal sinceOpening = contributedBalance.subtract(opening);

        BigDecimal currentValue = null;
        BigDecimal returnAmount = null;
        BigDecimal returnPct = null;
                AccountType accountType = account.getAccountType();

                if (accountType == AccountType.INVESTMENT) {
                    currentValue = account.getManualBalance() != null
                            ? account.getManualBalance()
                            : checkpointRepository
                                    .findTopByAccountIdOrderByRecordedAtDesc(account.getId())
                                    .map(c -> c.getValue())
                                    .orElse(contributedBalance);

            // For investment accounts, dashboard primary balance should reflect latest checkpoint market value.
            displayBalance = currentValue;
            sinceOpening = displayBalance.subtract(opening);

            returnAmount = currentValue.subtract(contributedBalance);
            if (contributedBalance.compareTo(BigDecimal.ZERO) > 0) {
                returnPct = returnAmount
                        .divide(contributedBalance, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
            }
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

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
                        return new BigDecimal(number.toString());
        }
        return BigDecimal.ZERO;
    }

        private BigDecimal manualDriftReference(AccountDashboardDTO account) {
                if ("INVESTMENT".equals(account.getAccountType()) && account.getCurrentValue() != null) {
                        return account.getCurrentValue();
                }
                return account.getCalculatedBalance();
        }

        private Object[] unwrapRow(Object[] row) {
                if (row == null) {
                        return null;
                }
                if (row.length == 1 && row[0] instanceof Object[] nested) {
                        return nested;
                }
                return row;
        }

    private String formatAmount(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        return (scaled.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + scaled;
    }
}
