package com.vfa.vault.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.entity.Account;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.ExpenseRepository;
import com.vfa.vault.repository.IncomeRepository;
import com.vfa.vault.repository.TransferRepository;
import com.vfa.vault.repository.projection.AccountIdAmountProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountBalanceService {

    private final AccountRepository accountRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final TransferRepository transferRepository;

    @Transactional(readOnly = true)
    public BalanceBreakdown getBreakdown(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        return getBreakdowns(List.of(account)).get(accountId);
    }

    @Transactional(readOnly = true)
    public Map<UUID, BalanceBreakdown> getBreakdowns(Collection<Account> accounts) {
        if (accounts.isEmpty()) {
            return Map.of();
        }

        List<UUID> accountIds = accounts.stream().map(Account::getId).toList();
        Map<UUID, BigDecimal> incomeByAccount = toAmountMap(incomeRepository.sumByAccountIds(accountIds));
        Map<UUID, BigDecimal> expensesByAccount = toAmountMap(expenseRepository.sumByAccountIds(accountIds));
        Map<UUID, BigDecimal> incomingByAccount = toAmountMap(transferRepository.sumIncomingByAccountIds(accountIds));
        Map<UUID, BigDecimal> outgoingByAccount = toAmountMap(transferRepository.sumOutgoingByAccountIds(accountIds));

        Map<UUID, BalanceBreakdown> breakdowns = new HashMap<>();
        for (Account account : accounts) {
            UUID accountId = account.getId();
            BigDecimal opening = defaultZero(account.getOpeningBalance());
            BigDecimal totalIncome = incomeByAccount.getOrDefault(accountId, BigDecimal.ZERO);
            BigDecimal totalExpenses = expensesByAccount.getOrDefault(accountId, BigDecimal.ZERO);
            BigDecimal incomingTransfers = incomingByAccount.getOrDefault(accountId, BigDecimal.ZERO);
            BigDecimal outgoingTransfers = outgoingByAccount.getOrDefault(accountId, BigDecimal.ZERO);
            BigDecimal calculatedBalance = opening
                    .add(totalIncome)
                    .subtract(totalExpenses)
                    .add(incomingTransfers)
                    .subtract(outgoingTransfers);

            breakdowns.put(accountId, new BalanceBreakdown(
                    opening,
                    totalIncome,
                    totalExpenses,
                    incomingTransfers,
                    outgoingTransfers,
                    calculatedBalance));
        }
        return breakdowns;
    }

    @Transactional(readOnly = true)
    public BigDecimal getCalculatedBalance(UUID accountId) {
        return getBreakdown(accountId).calculatedBalance();
    }

    private Map<UUID, BigDecimal> toAmountMap(List<AccountIdAmountProjection> rows) {
        return rows.stream()
                .collect(Collectors.toMap(
                        AccountIdAmountProjection::getAccountId,
                        row -> defaultZero(row.getTotal()),
                        (left, right) -> left));
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public record BalanceBreakdown(
            BigDecimal openingBalance,
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal incomingTransfers,
            BigDecimal outgoingTransfers,
            BigDecimal calculatedBalance
    ) {}
}
