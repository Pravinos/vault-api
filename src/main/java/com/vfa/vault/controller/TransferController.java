package com.vfa.vault.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vfa.vault.dto.TransferDTO;
import com.vfa.vault.dto.TransferResponseDTO;
import com.vfa.vault.service.TransferService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponseDTO> createTransfer(@Valid @RequestBody TransferDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.createTransfer(request));
    }

    @PostMapping("/{id}/revert")
    public ResponseEntity<TransferResponseDTO> revertTransfer(@PathVariable("id") java.util.UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.revertTransfer(id));
    }
}
