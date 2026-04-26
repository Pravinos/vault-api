package com.vfa.vault.controller;

import com.vfa.vault.dto.GoalDTO;
import com.vfa.vault.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalDTO.Response>> findAll() {
        return ResponseEntity.ok(goalService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalDTO.Response> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(goalService.findById(id));
    }

    @PostMapping
    public ResponseEntity<GoalDTO.Response> create(
            @Valid @RequestBody GoalDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalDTO.Response> update(
            @PathVariable UUID id,
            @Valid @RequestBody GoalDTO.Request request) {
        return ResponseEntity.ok(goalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        goalService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<GoalDTO.Response> contribute(
            @PathVariable UUID id,
            @Valid @RequestBody GoalDTO.ContributeRequest request) {
        return ResponseEntity.ok(goalService.contribute(id, request));
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<GoalDTO.Response> getProgress(@PathVariable UUID id) {
        return ResponseEntity.ok(goalService.findById(id));
    }
}
