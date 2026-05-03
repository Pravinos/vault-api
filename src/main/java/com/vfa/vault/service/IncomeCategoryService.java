package com.vfa.vault.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.IncomeCategoryDTO;
import com.vfa.vault.repository.IncomeCategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncomeCategoryService {

    private final IncomeCategoryRepository incomeCategoryRepository;

    @Transactional(readOnly = true)
    public List<IncomeCategoryDTO.Response> getAllCategories() {
        return incomeCategoryRepository.findAll().stream()
                .map(cat -> new IncomeCategoryDTO.Response(cat.getId(), cat.getName(), cat.getIcon()))
                .toList();
    }
}
