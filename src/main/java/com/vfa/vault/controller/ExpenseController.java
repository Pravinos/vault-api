package com.vfa.vault.controller;

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

import com.vfa.vault.dto.ExpenseDTO;
import com.vfa.vault.dto.ExpenseHeatmapDTO;
import com.vfa.vault.service.ExpenseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<ExpenseDTO.Response>> findAll(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer categoryId) {
        return ResponseEntity.ok(expenseService.findAll(month, categoryId));
    }

    @PostMapping
    public ResponseEntity<ExpenseDTO.Response> create(
            @Valid @RequestBody ExpenseDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDTO.Response> update(
            @PathVariable UUID id,
            @Valid @RequestBody ExpenseDTO.Request request) {
        return ResponseEntity.ok(expenseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        expenseService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Expense deleted"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ExpenseDTO.MonthlySummary> getMonthlySummary(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(expenseService.getMonthlySummary(month));
    }

    @GetMapping("/stats")
    public ResponseEntity<ExpenseDTO.Stats> getStats() {
        return ResponseEntity.ok(expenseService.getStats());
    }

    @GetMapping("/heatmap")
    public ResponseEntity<ExpenseHeatmapDTO> getHeatmap(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year
    ) {
        return ResponseEntity.ok(expenseService.getHeatmap(year));
    }
}