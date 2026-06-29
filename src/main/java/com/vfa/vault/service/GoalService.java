package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.GoalDTO;
import com.vfa.vault.entity.Account;
import com.vfa.vault.entity.AccountType;
import com.vfa.vault.entity.Goal;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.GoalRepository;
import com.vfa.vault.service.AccountBalanceService.BalanceBreakdown;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final InvestmentBalanceService investmentBalanceService;

    @Transactional(readOnly = true)
    public List<GoalDTO.Response> findAllActive() {
        List<Goal> goals = goalRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        GoalBalanceContext context = loadBalanceContext(goals);
        return goals.stream()
                .map(goal -> toResponse(goal, context))
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalDTO.Response findById(UUID id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        return toResponse(goal, loadBalanceContext(List.of(goal)));
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
            goal.setLinkedAccounts(resolveLinkedAccounts(request.accountIds()));
        }

        Goal saved = goalRepository.save(goal);
        return toResponse(saved, loadBalanceContext(List.of(saved)));
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
            goal.getLinkedAccounts().clear();
            if (!request.accountIds().isEmpty()) {
                goal.getLinkedAccounts().addAll(resolveLinkedAccounts(request.accountIds()));
            }
        }

        Goal saved = goalRepository.save(goal);
        return toResponse(saved, loadBalanceContext(List.of(saved)));
    }

    @Transactional
    public void deactivate(UUID id) {
        var goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        goal.setActive(false);
        goalRepository.save(goal);
    }

    @Transactional
    public GoalDTO.Response linkAccount(UUID goalId, UUID accountId) {
        var goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        goal.getLinkedAccounts().add(account);
        Goal saved = goalRepository.save(goal);
        return toResponse(saved, loadBalanceContext(List.of(saved)));
    }

    @Transactional
    public GoalDTO.Response unlinkAccount(UUID goalId, UUID accountId) {
        var goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        goal.getLinkedAccounts().remove(account);
        Goal saved = goalRepository.save(goal);
        return toResponse(saved, loadBalanceContext(List.of(saved)));
    }

    private GoalBalanceContext loadBalanceContext(Collection<Goal> goals) {
        List<Account> linkedAccounts = goals.stream()
                .flatMap(goal -> goal.getLinkedAccounts().stream())
                .distinct()
                .toList();
        if (linkedAccounts.isEmpty()) {
            return new GoalBalanceContext(Map.of(), Map.of());
        }

        Map<UUID, BalanceBreakdown> breakdowns = accountBalanceService.getBreakdowns(linkedAccounts);
        List<UUID> investmentIds = linkedAccounts.stream()
                .filter(account -> account.getAccountType() == AccountType.INVESTMENT)
                .map(Account::getId)
                .toList();
        Map<UUID, BigDecimal> checkpointValues =
                investmentBalanceService.loadLatestCheckpointValues(investmentIds);
        return new GoalBalanceContext(breakdowns, checkpointValues);
    }

    private GoalDTO.Response toResponse(Goal goal, GoalBalanceContext context) {
        List<GoalDTO.LinkedAccountSummary> linked = goal.getLinkedAccounts().stream()
                .map(account -> {
                    BalanceBreakdown breakdown = context.breakdowns().get(account.getId());
                    BigDecimal contributedBalance = breakdown != null
                            ? breakdown.calculatedBalance()
                            : BigDecimal.ZERO;
                    BigDecimal balance = investmentBalanceService.resolveCurrentValue(
                            account, contributedBalance, context.checkpointValues());
                    return new GoalDTO.LinkedAccountSummary(
                            account.getId(),
                            account.getName(),
                            account.getAccountType().name(),
                            balance);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        BigDecimal saved = linked.stream()
                .map(GoalDTO.LinkedAccountSummary::calculatedBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double progress = 0;
        if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progress = saved
                    .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        long daysRemaining = 0;
        if (goal.getDeadline() != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline());
            if (daysRemaining < 0) {
                daysRemaining = 0;
            }
        }

        boolean isOverdue = goal.getDeadline() != null
                && goal.getDeadline().isBefore(LocalDate.now())
                && saved.compareTo(goal.getTargetAmount() == null ? BigDecimal.ZERO : goal.getTargetAmount()) < 0;

        return new GoalDTO.Response(
                goal.getId(),
                goal.getName(),
                goal.getDescription(),
                goal.getTargetAmount(),
                saved,
                goal.getGoalType(),
                goal.getDeadline(),
                goal.getCreatedAt(),
                goal.isActive(),
                progress,
                daysRemaining,
                isOverdue,
                linked);
    }

    private Set<Account> resolveLinkedAccounts(Set<UUID> accountIds) {
        List<Account> accounts = accountRepository.findAllById(accountIds);
        if (accounts.size() != accountIds.size()) {
            throw new IllegalArgumentException("One or more account IDs are invalid");
        }
        return new HashSet<>(accounts);
    }

    private record GoalBalanceContext(
            Map<UUID, BalanceBreakdown> breakdowns,
            Map<UUID, BigDecimal> checkpointValues) {}
}
