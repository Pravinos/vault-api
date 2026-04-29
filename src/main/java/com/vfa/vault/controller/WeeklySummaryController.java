package com.vfa.vault.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vfa.vault.entity.WeeklySummary;
import com.vfa.vault.service.WeeklySummaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/summaries")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class WeeklySummaryController {

    private final WeeklySummaryService weeklySummaryService;

    @GetMapping
    public ResponseEntity<List<WeeklySummary>> findAll() {
        return ResponseEntity.ok(weeklySummaryService.findAll());
    }

    // Place /latest before /{id} to avoid path variable conflict
    @GetMapping("/latest")
    public ResponseEntity<WeeklySummary> findLatest() {
        return weeklySummaryService.findLatest()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeeklySummary> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(weeklySummaryService.findById(id));
    }
}
