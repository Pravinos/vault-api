package com.vfa.vault.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.entity.Account;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.ExpenseRepository;
import com.vfa.vault.repository.IncomeRepository;
import com.vfa.vault.repository.TransferRepository;

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

        BigDecimal totalIncome = defaultZero(incomeRepository.sumByAccountId(accountId));
        BigDecimal totalExpenses = defaultZero(expenseRepository.sumByAccountId(accountId));
        BigDecimal incomingTransfers = defaultZero(transferRepository.sumIncomingByAccountId(accountId));
        BigDecimal outgoingTransfers = defaultZero(transferRepository.sumOutgoingByAccountId(accountId));

        BigDecimal calculatedBalance = account.getOpeningBalance()
                .add(totalIncome)
                .subtract(totalExpenses)
                .add(incomingTransfers)
                .subtract(outgoingTransfers);

        return new BalanceBreakdown(
                account.getOpeningBalance(),
                totalIncome,
                totalExpenses,
                incomingTransfers,
                outgoingTransfers,
                calculatedBalance);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCalculatedBalance(UUID accountId) {
        return getBreakdown(accountId).calculatedBalance();
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
