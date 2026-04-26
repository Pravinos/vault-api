package com.vfa.vault.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.vfa.vault.dto.CategoryDTO;
import com.vfa.vault.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDTO.Response> findAll() {
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryDTO.Response(c.getId(), c.getName(), c.getIcon()))
                .toList();
    }
}