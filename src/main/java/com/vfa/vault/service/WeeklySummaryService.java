package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.vfa.vault.ai.LlmProviderRouter;
import com.vfa.vault.ai.WeeklyDataSnapshot;
import com.vfa.vault.dto.AccountDTO;
import com.vfa.vault.dto.GoalDTO;
import com.vfa.vault.dto.WeeklySummaryResponseDTO;
import com.vfa.vault.entity.LlmProviderConfig;
import com.vfa.vault.entity.WeeklySummary;
import com.vfa.vault.exception.LlmServiceUnavailableException;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.ExpenseRepository;
import com.vfa.vault.repository.IncomeRepository;
import com.vfa.vault.repository.LlmProviderConfigRepository;
import com.vfa.vault.repository.WeeklySummaryRepository;
import com.vfa.vault.repository.projection.CategoryAmountProjection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklySummaryService {

    private final WeeklySummaryRepository weeklySummaryRepository;
    private final LlmProviderConfigRepository configRepo;
    private final LlmProviderRouter llmProviderRouter;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final GoalService goalService;
    private final AccountService accountService;
    private final PlatformTransactionManager transactionManager;

    @Transactional(readOnly = true)
    public List<WeeklySummaryResponseDTO> findAll() {
        return weeklySummaryRepository.findAllByOrderByGeneratedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<WeeklySummaryResponseDTO> findLatest() {
        return weeklySummaryRepository.findTopByOrderByGeneratedAtDesc()
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WeeklySummaryResponseDTO findById(UUID id) {
        return weeklySummaryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("WeeklySummary", id));
    }

    public WeeklySummaryResponseDTO generateNow() {
        return generateAndSave()
                .map(this::toResponse)
                .orElseThrow(() -> new LlmServiceUnavailableException(
                        "Summary generation is currently unavailable. Verify LM Studio/Groq connectivity and credentials."));
    }

    public void generateScheduled() {
        Optional<WeeklySummary> generated = generateAndSave();
        if (generated.isPresent()) {
            WeeklySummary summary = generated.get();
            log.info("Weekly summary generated using {}/{}", summary.getProvider(), summary.getModel());
        } else {
            log.warn("Weekly summary generation skipped due to provider/model call failure");
        }
    }

    @Transactional
    public void deleteById(UUID id) {
        // Hard delete only. Idempotent by design: deleting a missing row is a no-op.
        weeklySummaryRepository.findById(id).ifPresent(summary -> {
            weeklySummaryRepository.delete(summary);
            weeklySummaryRepository.flush();
        });
    }

    private Optional<WeeklySummary> generateAndSave() {
        LlmProviderConfig config = loadConfig();
        WeeklyDataSnapshot snapshot = loadSnapshot();
        ChatClient client = llmProviderRouter.getClientForTask(LlmProviderRouter.TaskType.SUMMARY);

        String summaryText;
        try {
            summaryText = client.prompt()
                    .user(buildPrompt(snapshot))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Weekly summary generation failed: {}", e.getMessage(), e);
            return Optional.empty();
        }

        if (summaryText == null || summaryText.isBlank()) {
            log.warn("Weekly summary generation returned an empty response; skipping persistence");
            return Optional.empty();
        }

        return Optional.of(persistSummary(snapshot, summaryText, config));
    }

    private LlmProviderConfig loadConfig() {
        TransactionTemplate tx = readOnlyTransaction();
        return tx.execute(status -> configRepo.getConfig());
    }

    private WeeklyDataSnapshot loadSnapshot() {
        TransactionTemplate tx = readOnlyTransaction();
        return tx.execute(status -> buildSnapshot());
    }

    private WeeklySummary persistSummary(
            WeeklyDataSnapshot snapshot,
            String summaryText,
            LlmProviderConfig config) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            WeeklySummary summary = new WeeklySummary();
            summary.setWeekStart(snapshot.weekStart());
            summary.setWeekEnd(snapshot.weekEnd());
            summary.setSummaryText(summaryText);
            summary.setTotalSpent(snapshot.totalSpent());
            summary.setProvider(config.getSummaryProvider());
            summary.setModel(config.getSummaryModel());
            summary.setGeneratedAt(LocalDateTime.now());
            return weeklySummaryRepository.save(summary);
        });
    }

    private TransactionTemplate readOnlyTransaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setReadOnly(true);
        return tx;
    }

    private WeeklyDataSnapshot buildSnapshot() {
        LocalDate weekEnd = LocalDate.now();
        LocalDate weekStart = weekEnd.minusDays(6);

        Map<String, BigDecimal> spendingByCategory = expenseRepository
                .sumByCategoryBetweenDates(weekStart, weekEnd)
                .stream()
                .collect(Collectors.toMap(
                        CategoryAmountProjection::getName,
                        CategoryAmountProjection::getTotal,
                        (left, right) -> left,
                        LinkedHashMap::new));

        BigDecimal totalSpent = spendingByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> incomeByCategory = incomeRepository
                .sumByCategoryBetweenDates(weekStart, weekEnd)
                .stream()
                .collect(Collectors.toMap(
                        CategoryAmountProjection::getName,
                        CategoryAmountProjection::getTotal,
                        (left, right) -> left,
                        LinkedHashMap::new));

        BigDecimal totalIncome = incomeByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netCashFlow = totalIncome.subtract(totalSpent);

        List<WeeklyDataSnapshot.GoalSnapshotItem> goals = goalService.findAllActive()
                .stream()
                .map(this::toGoalSnapshot)
                .toList();

        List<WeeklyDataSnapshot.AccountSnapshotItem> accounts = accountService.getAllAccounts()
                .stream()
                .map(this::toAccountSnapshot)
                .toList();

        return new WeeklyDataSnapshot(
                weekStart,
                weekEnd,
                totalSpent,
                totalIncome,
                netCashFlow,
                spendingByCategory,
                incomeByCategory,
                goals,
                accounts);
    }

    private WeeklyDataSnapshot.GoalSnapshotItem toGoalSnapshot(GoalDTO.Response goal) {
        BigDecimal target = goal.targetAmount() != null ? goal.targetAmount() : BigDecimal.ZERO;
        BigDecimal saved = goal.savedAmount() != null ? goal.savedAmount() : BigDecimal.ZERO;

        BigDecimal percentage = target.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : saved.divide(target, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        Long daysRemaining = goal.deadline() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), goal.deadline())
                : null;

        return new WeeklyDataSnapshot.GoalSnapshotItem(
                goal.name(),
                target,
                saved,
                percentage,
                daysRemaining);
    }

    private WeeklyDataSnapshot.AccountSnapshotItem toAccountSnapshot(AccountDTO.Response account) {
        return new WeeklyDataSnapshot.AccountSnapshotItem(
                account.name(),
                account.accountType().name(),
                account.calculatedBalance(),
                account.manualBalance(),
                account.contributedAmount(),
                account.currentValue(),
                account.returnPercentage());
    }

    private String buildPrompt(WeeklyDataSnapshot snapshot) {
        return """
                You are Vault, a personal finance assistant. Analyse this week's financial data and write a concise, friendly summary.

                Period: %s to %s

                SPENDING
                Total spent: €%.2f
                By category: %s

                INCOME
                Total income: €%.2f
                By category: %s

                NET CASH FLOW: €%.2f

                ACCOUNTS
                %s

                GOALS
                %s

                Write a summary of 4-6 sentences covering:
                1. Where most money was spent this week
                2. Income vs spending balance
                3. One practical, specific tip based on the data
                4. A brief note on goal progress if any goals exist
                5. If any investment account has a notable return (positive or negative), mention it

                Be specific with amounts. Use the € symbol. Do not use bullet points - write in natural prose paragraphs.
                """.formatted(
                snapshot.weekStart(),
                snapshot.weekEnd(),
                snapshot.totalSpent(),
                formatCategoryMap(snapshot.spendingByCategory()),
                snapshot.totalIncome(),
                formatCategoryMap(snapshot.incomeByCategory()),
                snapshot.netCashFlow(),
                formatAccounts(snapshot.accounts()),
                formatGoals(snapshot.goals()));
    }

    private String formatCategoryMap(Map<String, BigDecimal> map) {
        if (map.isEmpty()) {
            return "none";
        }

        return map.entrySet().stream()
                .map(e -> e.getKey() + ": €" + e.getValue().setScale(2, RoundingMode.HALF_UP))
                .collect(Collectors.joining(", "));
    }

    private String formatAccounts(List<WeeklyDataSnapshot.AccountSnapshotItem> accounts) {
        if (accounts.isEmpty()) {
            return "No active accounts.";
        }

        return accounts.stream()
                .map(account -> {
                    String base = "- %s (%s): calculated €%.2f, manual €%.2f".formatted(
                            account.name(),
                            account.type(),
                            account.calculatedBalance(),
                            account.manualBalance() != null
                                    ? account.manualBalance()
                                    : account.calculatedBalance());

                    if ("INVESTMENT".equals(account.type()) && account.currentValue() != null) {
                        base += ", current value €%.2f (return: %.2f%%)".formatted(
                                account.currentValue(),
                                account.returnPercentage() != null
                                        ? account.returnPercentage()
                                        : BigDecimal.ZERO);
                    }
                    return base;
                })
                .collect(Collectors.joining("\n"));
    }

    private String formatGoals(List<WeeklyDataSnapshot.GoalSnapshotItem> goals) {
        if (goals.isEmpty()) {
            return "No active goals.";
        }

        return goals.stream()
                .map(goal -> "- %s: €%.2f / €%.2f (%.1f%%)%s".formatted(
                        goal.name(),
                        goal.saved(),
                        goal.target(),
                        goal.percentage(),
                        goal.daysRemaining() != null ? ", %d days remaining".formatted(goal.daysRemaining()) : ""))
                .collect(Collectors.joining("\n"));
    }

    private WeeklySummaryResponseDTO toResponse(WeeklySummary summary) {
        return new WeeklySummaryResponseDTO(
                summary.getId(),
                summary.getWeekStart(),
                summary.getWeekEnd(),
                summary.getSummaryText(),
                summary.getTotalSpent(),
                summary.getGeneratedAt(),
                summary.getProvider(),
                summary.getModel());
    }
}
