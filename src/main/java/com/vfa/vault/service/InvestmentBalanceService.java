package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.vfa.vault.entity.Account;
import com.vfa.vault.entity.AccountType;
import com.vfa.vault.entity.InvestmentCheckpoint;
import com.vfa.vault.repository.InvestmentCheckpointRepository;
import com.vfa.vault.repository.projection.AccountIdAmountProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvestmentBalanceService {

    private final InvestmentCheckpointRepository checkpointRepository;

    public BigDecimal resolveCurrentValue(Account account, BigDecimal contributedBalance) {
        if (account.getAccountType() != AccountType.INVESTMENT) {
            return contributedBalance;
        }
        if (account.getManualBalance() != null) {
            return account.getManualBalance();
        }
        return checkpointRepository.findTopByAccountIdOrderByRecordedAtDesc(account.getId())
                .map(InvestmentCheckpoint::getValue)
                .orElse(contributedBalance);
    }

    public BigDecimal resolveCurrentValue(
            Account account,
            BigDecimal contributedBalance,
            Map<UUID, BigDecimal> latestCheckpointValues) {
        if (account.getAccountType() != AccountType.INVESTMENT) {
            return contributedBalance;
        }
        if (account.getManualBalance() != null) {
            return account.getManualBalance();
        }
        return latestCheckpointValues.getOrDefault(account.getId(), contributedBalance);
    }

    public Map<UUID, BigDecimal> loadLatestCheckpointValues(Collection<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return checkpointRepository.findLatestValuesByAccountIds(accountIds).stream()
                .collect(Collectors.toMap(
                        AccountIdAmountProjection::getAccountId,
                        row -> defaultZero(row.getTotal()),
                        (left, right) -> left));
    }

    public BigDecimal resolveSnapshotBase(Account account) {
        if (account.getManualBalance() != null) {
            return account.getManualBalance();
        }
        return checkpointRepository.findTopByAccountIdOrderByRecordedAtDesc(account.getId())
                .map(InvestmentCheckpoint::getValue)
                .orElse(defaultZero(account.getOpeningBalance()));
    }

    public BigDecimal computeReturnAmount(BigDecimal contributedBalance, BigDecimal currentValue) {
        return currentValue.subtract(contributedBalance);
    }

    public BigDecimal computeReturnPercentage(BigDecimal contributedBalance, BigDecimal currentValue) {
        if (contributedBalance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return computeReturnAmount(contributedBalance, currentValue)
                .divide(contributedBalance, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal computeReturnPercentageForDisplay(
            BigDecimal contributedBalance,
            BigDecimal currentValue) {
        if (contributedBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return computeReturnAmount(contributedBalance, currentValue)
                .divide(contributedBalance, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
