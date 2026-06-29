package com.vfa.vault.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.AccountDTO;
import com.vfa.vault.entity.Account;
import com.vfa.vault.entity.AccountType;
import com.vfa.vault.entity.InvestmentDetail;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.InvestmentDetailRepository;
import com.vfa.vault.service.AccountBalanceService.BalanceBreakdown;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final InvestmentDetailRepository investmentDetailRepository;
    private final AccountBalanceService accountBalanceService;
    private final InvestmentBalanceService investmentBalanceService;

    @Transactional(readOnly = true)
    public List<AccountDTO.Response> getAllAccounts() {
        List<Account> accounts = accountRepository.findAllOrderByLastUpdatedDesc();
        AccountViewContext context = loadViewContext(accounts);
        return accounts.stream()
                .map(account -> buildResponse(account, context))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountDTO.Response getAccountById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        return buildResponse(account, loadViewContext(List.of(account)));
    }

    @Transactional
    public AccountDTO.Response createAccount(AccountDTO.Request dto) {
        var account = new Account();
        account.setName(dto.name());
        account.setAccountType(dto.accountType());
        account.setOpeningBalance(dto.openingBalance());

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

        return buildResponse(account, loadViewContext(List.of(account)));
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
            investmentDetailRepository.findByAccountId(account.getId())
                    .ifPresent(investmentDetailRepository::delete);
        }

        return buildResponse(account, loadViewContext(List.of(account)));
    }

    @Transactional
    public void deleteAccount(UUID id) {
        if (!accountRepository.existsById(id)) {
            return;
        }

        investmentDetailRepository.deleteByAccountId(id);

        try {
            accountRepository.deleteById(id);
            accountRepository.flush();
        } catch (DataIntegrityViolationException ex) {
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
        }

        account = accountRepository.save(account);
        return buildResponse(account, loadViewContext(List.of(account)));
    }

    private AccountViewContext loadViewContext(Collection<Account> accounts) {
        Map<UUID, BalanceBreakdown> breakdowns = accountBalanceService.getBreakdowns(accounts);
        List<UUID> investmentIds = accounts.stream()
                .filter(account -> account.getAccountType() == AccountType.INVESTMENT)
                .map(Account::getId)
                .toList();
        Map<UUID, BigDecimal> checkpointValues =
                investmentBalanceService.loadLatestCheckpointValues(investmentIds);
        Map<UUID, InvestmentDetail> details = investmentIds.isEmpty()
                ? Map.of()
                : investmentDetailRepository.findByAccountIdIn(investmentIds).stream()
                        .collect(Collectors.toMap(detail -> detail.getAccount().getId(), detail -> detail));
        return new AccountViewContext(breakdowns, checkpointValues, details);
    }

    private AccountDTO.Response buildResponse(Account account, AccountViewContext context) {
        BalanceBreakdown breakdown = context.breakdowns().get(account.getId());
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
            InvestmentDetail detail = context.details().get(account.getId());
            if (detail != null) {
                platform = detail.getPlatform();
                instrument = detail.getInstrument();
                assetType = detail.getAssetType();
            }

            contributedAmount = contributedBalance;
            currentValue = investmentBalanceService.resolveCurrentValue(
                    account, contributedBalance, context.checkpointValues());
            displayCalculatedBalance = currentValue;
            returnAmount = investmentBalanceService.computeReturnAmount(contributedAmount, currentValue);
            returnPercentage = investmentBalanceService.computeReturnPercentage(
                    contributedAmount, currentValue);
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

    private record AccountViewContext(
            Map<UUID, BalanceBreakdown> breakdowns,
            Map<UUID, BigDecimal> checkpointValues,
            Map<UUID, InvestmentDetail> details) {}
}
