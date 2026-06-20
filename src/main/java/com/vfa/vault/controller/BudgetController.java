package com.vfa.vault.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vfa.vault.dto.BudgetDTO;
import com.vfa.vault.dto.BudgetSummaryDTO;
import com.vfa.vault.service.BudgetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetDTO.Response>> getBudgets(
            @RequestParam(required = true) String month) {
        return ResponseEntity.ok(budgetService.getBudgetsForMonth(month));
    }

    @PostMapping
    public ResponseEntity<BudgetDTO.Response> upsertBudget(
            @Valid @RequestBody BudgetDTO.Request request) {
        return ResponseEntity.ok(budgetService.upsertBudget(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<List<BudgetSummaryDTO>> getSummary(
            @RequestParam(required = true) String month) {
        return ResponseEntity.ok(budgetService.getBudgetSummary(month));
    }
}
