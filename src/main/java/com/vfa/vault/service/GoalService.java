package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.GoalDTO;
import com.vfa.vault.entity.Goal;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.GoalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;

    public List<GoalDTO.Response> findAllActive() {
        return goalRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public GoalDTO.Response findById(UUID id) {
        return goalRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
    }

    @Transactional
    public GoalDTO.Response create(GoalDTO.Request request) {
        var goal = new Goal();
        goal.setName(request.name());
        goal.setDescription(request.description());
        goal.setTargetAmount(request.targetAmount());
        goal.setGoalType(request.goalType());
        goal.setDeadline(request.deadline());
        goal.setSavedAmount(BigDecimal.ZERO);
        goal.setIsActive(true);

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalDTO.Response update(UUID id, GoalDTO.Request request) {
        var goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));

        goal.setName(request.name());
        goal.setDescription(request.description());
        goal.setTargetAmount(request.targetAmount());
        goal.setGoalType(request.goalType());
        goal.setDeadline(request.deadline());

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public void deactivate(UUID id) {
        var goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        goal.setIsActive(false);
        goalRepository.save(goal);
    }

    @Transactional
    public GoalDTO.Response contribute(UUID id, GoalDTO.ContributeRequest request) {
        var goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));

        BigDecimal newSaved = goal.getSavedAmount().add(request.amount());
        goal.setSavedAmount(newSaved);

        return toResponse(goalRepository.save(goal));
    }

    public List<GoalDTO.Response> getAllProgress() {
        return findAllActive();
    }

    private GoalDTO.Response toResponse(Goal g) {
        double progress = 0;
        if (g.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progress = g.getSavedAmount()
                    .divide(g.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        long daysRemaining = 0;
        if (g.getDeadline() != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), g.getDeadline());
            if (daysRemaining < 0) daysRemaining = 0;
        }

        return new GoalDTO.Response(
                g.getId(),
                g.getName(),
                g.getDescription(),
                g.getTargetAmount(),
                g.getSavedAmount(),
                g.getGoalType(),
                g.getDeadline(),
                g.getCreatedAt(),
                g.getIsActive(),
                progress,
                daysRemaining);
    }
}