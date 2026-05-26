package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.GoalDTO;
import com.vfa.vault.entity.Account;
import com.vfa.vault.entity.Goal;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.GoalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;

    @Transactional(readOnly = true)
    public List<GoalDTO.Response> findAllActive() {
        return goalRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
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

        if (request.accountIds() != null && !request.accountIds().isEmpty()) {
            List<Account> accounts = accountRepository.findAllById(request.accountIds());
            goal.setLinkedAccounts(new HashSet<>(accounts));
        }

        // savedAmount remains for backward compatibility but is no longer authoritative
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

        if (request.accountIds() != null) {
            // replace linked accounts
            List<Account> accounts = accountRepository.findAllById(request.accountIds());
            goal.getLinkedAccounts().clear();
            goal.getLinkedAccounts().addAll(accounts);
        }

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
    public GoalDTO.Response linkAccount(UUID goalId, UUID accountId) {
        var goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        goal.getLinkedAccounts().add(account);
        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalDTO.Response unlinkAccount(UUID goalId, UUID accountId) {
        var goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        goal.getLinkedAccounts().remove(account);
        return toResponse(goalRepository.save(goal));
    }

    private GoalDTO.Response toResponse(Goal g) {
        // derive savedAmount from linked account balances
        List<GoalDTO.LinkedAccountSummary> linked = g.getLinkedAccounts().stream()
            .map(a -> {
                BigDecimal balance = accountBalanceService.getCalculatedBalance(a.getId());
                return new GoalDTO.LinkedAccountSummary(
                    a.getId(),
                    a.getName(),
                    a.getAccountType().name(),
                    balance);
            }).collect(Collectors.toList());

        BigDecimal saved = linked.stream()
            .map(GoalDTO.LinkedAccountSummary::calculatedBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        double progress = 0;
        if (g.getTargetAmount() != null && g.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progress = saved
                    .divide(g.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        long daysRemaining = 0;
        if (g.getDeadline() != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), g.getDeadline());
            if (daysRemaining < 0) daysRemaining = 0;
        }

        boolean isOverdue = g.getDeadline() != null
                && g.getDeadline().isBefore(LocalDate.now())
                && saved.compareTo(g.getTargetAmount() == null ? BigDecimal.ZERO : g.getTargetAmount()) < 0;

        return new GoalDTO.Response(
                g.getId(),
                g.getName(),
                g.getDescription(),
                g.getTargetAmount(),
                saved,
                g.getGoalType(),
                g.getDeadline(),
                g.getCreatedAt(),
                g.getIsActive(),
                progress,
                daysRemaining,
                isOverdue,
                linked);
    }
}