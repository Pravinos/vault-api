package com.vfa.vault.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.WeeklySummaryDTO;
import com.vfa.vault.entity.WeeklySummary;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.WeeklySummaryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeeklySummaryService {

    private final WeeklySummaryRepository weeklySummaryRepository;

    @Transactional(readOnly = true)
    public List<WeeklySummaryDTO.Response> findAll() {
        return weeklySummaryRepository.findAllByOrderByGeneratedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<WeeklySummaryDTO.Response> findLatest() {
        return weeklySummaryRepository.findTopByOrderByGeneratedAtDesc()
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WeeklySummaryDTO.Response findById(UUID id) {
        return weeklySummaryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("WeeklySummary", id));
    }

    private WeeklySummaryDTO.Response toResponse(WeeklySummary w) {
        return new WeeklySummaryDTO.Response(
                w.getId(),
                w.getWeekStart(),
                w.getWeekEnd(),
                w.getSummaryText(),
                w.getTotalSpent(),
                w.getGeneratedAt(),
                w.getProvider()
        );
    }
}
