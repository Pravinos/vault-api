package com.vfa.vault.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vfa.vault.dto.AccountDTO;
import com.vfa.vault.dto.InvestmentCheckpointDTO;
import com.vfa.vault.service.AccountService;
import com.vfa.vault.service.InvestmentCheckpointService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final InvestmentCheckpointService investmentCheckpointService;

    @GetMapping
    public ResponseEntity<List<AccountDTO.Response>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO.Response> getAccountById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @PostMapping
    public ResponseEntity<AccountDTO.Response> createAccount(
            @Valid @RequestBody AccountDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDTO.Response> updateAccount(
            @PathVariable UUID id,
            @Valid @RequestBody AccountDTO.Request request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAccount(@PathVariable UUID id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }

    @PatchMapping("/{id}/manual-balance")
    public ResponseEntity<AccountDTO.Response> updateManualBalance(
            @PathVariable UUID id,
            @Valid @RequestBody AccountDTO.ManualBalanceRequest request) {
        return ResponseEntity.ok(accountService.updateManualBalance(id, request));
    }

    @GetMapping("/{id}/checkpoints")
    public ResponseEntity<List<InvestmentCheckpointDTO.Response>> getCheckpoints(
            @PathVariable UUID id) {
        return ResponseEntity.ok(investmentCheckpointService.getCheckpoints(id));
    }

    @PostMapping("/{id}/checkpoints")
    public ResponseEntity<InvestmentCheckpointDTO.Response> addCheckpoint(
            @PathVariable UUID id,
            @Valid @RequestBody InvestmentCheckpointDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(investmentCheckpointService.addCheckpoint(id, request));
    }
}
