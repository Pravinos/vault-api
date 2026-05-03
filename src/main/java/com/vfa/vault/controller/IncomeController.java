package com.vfa.vault.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vfa.vault.dto.IncomeDTO;
import com.vfa.vault.service.IncomeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/income")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    @GetMapping
    public ResponseEntity<List<IncomeDTO.Response>> getIncome(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) UUID accountId) {
        return ResponseEntity.ok(incomeService.getIncome(month, accountId));
    }

    @PostMapping
    public ResponseEntity<IncomeDTO.Response> createIncome(
            @Valid @RequestBody IncomeDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeService.createIncome(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomeDTO.Response> updateIncome(
            @PathVariable UUID id,
            @Valid @RequestBody IncomeDTO.Request request) {
        return ResponseEntity.ok(incomeService.updateIncome(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable UUID id) {
        incomeService.deleteIncome(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, BigDecimal>> getMonthlySummary(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(incomeService.getMonthlySummary(month));
    }
}
