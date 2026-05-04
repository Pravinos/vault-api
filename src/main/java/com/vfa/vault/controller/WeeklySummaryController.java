package com.vfa.vault.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vfa.vault.dto.WeeklySummaryResponseDTO;
import com.vfa.vault.service.WeeklySummaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ai/summaries")
@RequiredArgsConstructor
public class WeeklySummaryController {

    private final WeeklySummaryService weeklySummaryService;

    @GetMapping
    public ResponseEntity<List<WeeklySummaryResponseDTO>> findAll() {
        return ResponseEntity.ok(weeklySummaryService.findAll());
    }

    // Place /latest before /{id} to avoid path variable conflict
    @GetMapping("/latest")
    public ResponseEntity<WeeklySummaryResponseDTO> findLatest() {
        return weeklySummaryService.findLatest()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/generate")
    public ResponseEntity<WeeklySummaryResponseDTO> generateNow() {
        return ResponseEntity.ok(weeklySummaryService.generateNow());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeeklySummaryResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(weeklySummaryService.findById(id));
    }
}
