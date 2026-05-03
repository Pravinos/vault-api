package com.vfa.vault.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vfa.vault.dto.IncomeCategoryDTO;
import com.vfa.vault.service.IncomeCategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/income-categories")
@RequiredArgsConstructor
public class IncomeCategoryController {

    private final IncomeCategoryService incomeCategoryService;

    @GetMapping
    public ResponseEntity<List<IncomeCategoryDTO.Response>> getAllCategories() {
        return ResponseEntity.ok(incomeCategoryService.getAllCategories());
    }
}
