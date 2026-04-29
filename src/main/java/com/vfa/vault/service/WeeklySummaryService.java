package com.vfa.vault.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vfa.vault.entity.WeeklySummary;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.WeeklySummaryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeeklySummaryService {

    private final WeeklySummaryRepository weeklySummaryRepository;

    public List<WeeklySummary> findAll() {
        return weeklySummaryRepository.findAllByOrderByGeneratedAtDesc();
    }


    public Optional<WeeklySummary> findLatest() {
        return weeklySummaryRepository.findTopByOrderByGeneratedAtDesc();
    }

    public WeeklySummary findById(UUID id) {
        return weeklySummaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WeeklySummary", id));
    }
}
