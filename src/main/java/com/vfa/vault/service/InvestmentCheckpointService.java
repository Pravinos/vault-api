package com.vfa.vault.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.InvestmentCheckpointDTO;
import com.vfa.vault.entity.AccountType;
import com.vfa.vault.entity.InvestmentCheckpoint;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.InvestmentCheckpointRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvestmentCheckpointService {

    private final InvestmentCheckpointRepository investmentCheckpointRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<InvestmentCheckpointDTO.Response> getCheckpoints(UUID accountId) {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        if (account.getAccountType() != AccountType.INVESTMENT) {
            throw new IllegalArgumentException("Account is not an investment account");
        }
        return investmentCheckpointRepository
                .findByAccountIdOrderByRecordedAtDesc(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InvestmentCheckpointDTO.Response addCheckpoint(UUID accountId, InvestmentCheckpointDTO.Request dto) {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        if (account.getAccountType() != AccountType.INVESTMENT) {
            throw new IllegalArgumentException("Account is not an investment account");
        }

        var checkpoint = new InvestmentCheckpoint();
        checkpoint.setAccount(account);
        checkpoint.setValue(dto.value());
        checkpoint.setNote(dto.note());

        checkpoint = investmentCheckpointRepository.save(checkpoint);

        // Keep the account's primary balance aligned with the latest investment checkpoint.
        account.setManualBalance(checkpoint.getValue());
        account.setManualBalanceUpdatedAt(checkpoint.getRecordedAt());
        accountRepository.save(account);

        return toResponse(checkpoint);
    }

    private InvestmentCheckpointDTO.Response toResponse(InvestmentCheckpoint c) {
        return new InvestmentCheckpointDTO.Response(
                c.getId(),
                c.getValue(),
                c.getRecordedAt(),
                c.getNote()
        );
    }
}
