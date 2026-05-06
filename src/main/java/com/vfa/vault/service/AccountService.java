package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.AccountDTO;
import com.vfa.vault.entity.Account;
import com.vfa.vault.entity.AccountType;
import com.vfa.vault.entity.InvestmentCheckpoint;
import com.vfa.vault.entity.InvestmentDetail;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.InvestmentCheckpointRepository;
import com.vfa.vault.repository.InvestmentDetailRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final InvestmentDetailRepository investmentDetailRepository;
    private final InvestmentCheckpointRepository investmentCheckpointRepository;
    private final AccountBalanceService accountBalanceService;

    @Transactional(readOnly = true)
    public List<AccountDTO.Response> getAllAccounts() {
        return accountRepository.findAllOrderByLastUpdatedDesc().stream()
                .map(this::buildResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountDTO.Response getAccountById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        return buildResponse(account);
    }

    @Transactional
    public AccountDTO.Response createAccount(AccountDTO.Request dto) {
        var account = new Account();
        account.setName(dto.name());
        account.setAccountType(dto.accountType());
        account.setOpeningBalance(dto.openingBalance());

        // If an opening balance is provided, use it as the initial manual balance too
        if (dto.openingBalance().compareTo(BigDecimal.ZERO) > 0) {
            account.setManualBalance(dto.openingBalance());
            account.setManualBalanceUpdatedAt(LocalDateTime.now());
        }

        account = accountRepository.save(account);

        if (dto.accountType() == AccountType.INVESTMENT
                && (dto.platform() != null || dto.instrument() != null || dto.assetType() != null)) {
            var detail = new InvestmentDetail();
            detail.setAccount(account);
            detail.setPlatform(dto.platform());
            detail.setInstrument(dto.instrument());
            detail.setAssetType(dto.assetType());
            investmentDetailRepository.save(detail);
        }

        return buildResponse(account);
    }

    @Transactional
    public AccountDTO.Response updateAccount(UUID id, AccountDTO.Request dto) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));

        account.setName(dto.name());
        account.setAccountType(dto.accountType());
        account.setOpeningBalance(dto.openingBalance());
        account = accountRepository.save(account);

        if (dto.accountType() == AccountType.INVESTMENT) {
            var detail = investmentDetailRepository.findByAccountId(account.getId())
                    .orElse(new InvestmentDetail());
            detail.setAccount(account);
            detail.setPlatform(dto.platform());
            detail.setInstrument(dto.instrument());
            detail.setAssetType(dto.assetType());
            investmentDetailRepository.save(detail);
        } else {
            // Clean up orphaned InvestmentDetail if type changed away from INVESTMENT
            investmentDetailRepository.findByAccountId(account.getId())
                    .ifPresent(investmentDetailRepository::delete);
        }

        return buildResponse(account);
    }

    @Transactional
    public void deleteAccount(UUID id) {
        // DELETE is idempotent: deleting an already missing account is a no-op.
        if (!accountRepository.existsById(id)) {
            return;
        }

        // Investment detail has a 1:1 FK to account and can be safely cleaned up.
        investmentDetailRepository.deleteByAccountId(id);

        try {
            accountRepository.deleteById(id);
            // Force constraint checks now so any DB error is handled in this method.
            accountRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // In rare race/commit-timing cases the row can already be gone despite exception.
            if (!accountRepository.existsById(id)) {
                return;
            }
            throw new IllegalArgumentException(
                    "Cannot delete account with linked transactions or checkpoints");
        }
    }

    @Transactional
    public AccountDTO.Response updateManualBalance(UUID id, AccountDTO.ManualBalanceRequest dto) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        account.setManualBalance(dto.manualBalance());
        account.setManualBalanceUpdatedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(dto.alsoSetAsOpeningBalance())) {
            var breakdown = accountBalanceService.getBreakdown(id);
            boolean hasNoTransactions =
                    breakdown.totalIncome().compareTo(BigDecimal.ZERO) == 0
                        && breakdown.totalExpenses().compareTo(BigDecimal.ZERO) == 0
                        && breakdown.incomingTransfers().compareTo(BigDecimal.ZERO) == 0
                        && breakdown.outgoingTransfers().compareTo(BigDecimal.ZERO) == 0;
            if (hasNoTransactions) {
                account.setOpeningBalance(dto.manualBalance());
            }
            // If transactions exist, silently ignore the flag — don't corrupt history
        }

        account = accountRepository.save(account);
        return buildResponse(account);
    }

    private AccountDTO.Response buildResponse(Account account) {
        var breakdown = accountBalanceService.getBreakdown(account.getId());
        BigDecimal totalIncome = breakdown.totalIncome();
        BigDecimal totalExpenses = breakdown.totalExpenses();
        BigDecimal contributedBalance = breakdown.calculatedBalance();
        BigDecimal displayCalculatedBalance = contributedBalance;

        String platform = null;
        String instrument = null;
        String assetType = null;
        BigDecimal contributedAmount = null;
        BigDecimal currentValue = null;
        BigDecimal returnAmount = null;
        BigDecimal returnPercentage = null;

        if (account.getAccountType() == AccountType.INVESTMENT) {
            var detailOpt = investmentDetailRepository.findByAccountId(account.getId());
            if (detailOpt.isPresent()) {
                var detail = detailOpt.get();
                platform = detail.getPlatform();
                instrument = detail.getInstrument();
                assetType = detail.getAssetType();
            }

            contributedAmount = contributedBalance;
            currentValue = account.getManualBalance() != null
                    ? account.getManualBalance()
                    : investmentCheckpointRepository
                            .findTopByAccountIdOrderByRecordedAtDesc(account.getId())
                            .map(InvestmentCheckpoint::getValue)
                            .orElse(contributedAmount);

            // For investment accounts, the primary displayed balance follows current manual/checkpoint snapshot.
            displayCalculatedBalance = currentValue;

            returnAmount = currentValue.subtract(contributedAmount);
            returnPercentage = contributedAmount.compareTo(BigDecimal.ZERO) != 0
                    ? returnAmount.divide(contributedAmount, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
        }

        return new AccountDTO.Response(
                account.getId(),
                account.getName(),
                account.getAccountType(),
                account.getOpeningBalance(),
                account.getManualBalance(),
                account.getManualBalanceUpdatedAt(),
                account.getCreatedAt(),
                displayCalculatedBalance,
                totalIncome,
                totalExpenses,
                platform,
                instrument,
                assetType,
                contributedAmount,
                currentValue,
                returnAmount,
                returnPercentage
        );
    }
}
