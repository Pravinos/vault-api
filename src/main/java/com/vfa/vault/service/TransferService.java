package com.vfa.vault.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.TransferDTO;
import com.vfa.vault.dto.TransferResponseDTO;
import com.vfa.vault.entity.Account;
import com.vfa.vault.entity.AccountType;
import com.vfa.vault.entity.Transfer;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.TransferRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger LOG = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final InvestmentBalanceService investmentBalanceService;

    @Transactional
    public TransferResponseDTO createTransfer(TransferDTO dto) {
        if (dto.fromAccountId().equals(dto.toAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        // Accounts no longer have an active flag (removed in V16), so existence is the rule.
        Account fromAccount = accountRepository.findById(dto.fromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", dto.fromAccountId()));
        Account toAccount = accountRepository.findById(dto.toAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", dto.toAccountId()));

        BigDecimal fromCalculatedBalance = accountBalanceService.getCalculatedBalance(fromAccount.getId());
        if (fromCalculatedBalance.compareTo(dto.amount()) < 0) {
            LOG.warn("Transfer amount {} exceeds calculated balance {} for account {}",
                    dto.amount(), fromCalculatedBalance, fromAccount.getId());
        }

        var transfer = new Transfer();
        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);
        transfer.setAmount(dto.amount());
        transfer.setNote(dto.note());
        transfer.setTransferDate(dto.transferDate() != null ? dto.transferDate() : LocalDate.now());

        Transfer saved = transferRepository.save(transfer);
        applyInvestmentSnapshotDelta(saved.getFromAccount(), saved.getAmount().negate());
        applyInvestmentSnapshotDelta(saved.getToAccount(), saved.getAmount());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransferResponseDTO> getTransfersForAccount(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account", accountId);
        }

        return transferRepository.findByAccountId(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TransferResponseDTO revertTransfer(UUID transferId) {
        Transfer original = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", transferId));

        if (original.isReversal()) {
            throw new IllegalArgumentException("Cannot revert a reversal transfer");
        }

        if (original.getOriginalTransfer() != null) {
            throw new IllegalArgumentException("Cannot revert a transfer that is already a reversal");
        }

        if (transferRepository.existsByOriginalTransferId(original.getId())) {
            throw new IllegalArgumentException("Transfer has already been reverted");
        }

        Account reversalFrom = original.getToAccount();
        Account reversalTo = original.getFromAccount();

        BigDecimal reversalFromBalance = accountBalanceService.getCalculatedBalance(reversalFrom.getId());
        if (reversalFromBalance.compareTo(original.getAmount()) < 0) {
            LOG.warn("Reversal amount {} exceeds calculated balance {} for account {}",
                    original.getAmount(), reversalFromBalance, reversalFrom.getId());
        }

        Transfer reversal = new Transfer();
        reversal.setFromAccount(reversalFrom);
        reversal.setToAccount(reversalTo);
        reversal.setAmount(original.getAmount());
        reversal.setTransferDate(LocalDate.now());
        reversal.setOriginalTransfer(original);
        reversal.setReversal(true);

        String originalNote = original.getNote() != null ? original.getNote() : "";
        String reversalNote = ("Reversal of transfer " + original.getId() + ": " + originalNote).trim();
        if (reversalNote.length() > 255) {
            reversalNote = reversalNote.substring(0, 255);
        }
        reversal.setNote(reversalNote);

        Transfer saved = transferRepository.save(reversal);
        applyInvestmentSnapshotDelta(saved.getFromAccount(), saved.getAmount().negate());
        applyInvestmentSnapshotDelta(saved.getToAccount(), saved.getAmount());
        return toResponse(saved);
    }

    private void applyInvestmentSnapshotDelta(Account account, BigDecimal delta) {
        if (account.getAccountType() != AccountType.INVESTMENT) {
            return;
        }

        BigDecimal updated = investmentBalanceService.resolveSnapshotBase(account).add(delta);
        account.setManualBalance(updated);
        account.setManualBalanceUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    private TransferResponseDTO toResponse(Transfer transfer) {
        return new TransferResponseDTO(
                transfer.getId(),
                transfer.getFromAccount().getName(),
                transfer.getToAccount().getName(),
                transfer.getAmount(),
                transfer.getNote(),
                transfer.getTransferDate(),
                transfer.getCreatedAt());
    }

}
