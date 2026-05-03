package com.vfa.vault.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.ai.LlmProviderRouter;
import com.vfa.vault.dto.WeeklySummaryDTO;
import com.vfa.vault.entity.Income;
import com.vfa.vault.entity.LlmProviderConfig;
import com.vfa.vault.entity.WeeklySummary;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.ExpenseRepository;
import com.vfa.vault.repository.IncomeRepository;
import com.vfa.vault.repository.LlmProviderConfigRepository;
import com.vfa.vault.repository.WeeklySummaryRepository;

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

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

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

        @Transactional
        public WeeklySummaryDTO.Response generate() {
        log.info("Starting weekly summary generation...");
        try {
            log.info("Loading LLM config...");
            LlmProviderConfig config = configRepo.findById(1)
                .orElseThrow(() -> new IllegalStateException("llm_provider_config row not found"));
            log.info("Using provider={}, model={}", config.getSummaryProvider(), config.getSummaryModel());

            log.info("Building snapshot...");
            WeeklyDataSnapshot snapshot = buildSnapshot();
            log.info("Snapshot built for week {} to {}", snapshot.weekStart(), snapshot.weekEnd());

            String prompt = """
                Here is the user's financial data for the past week (%s to %s):

                Total spent: EUR %.2f
                Total income: EUR %.2f
                Net cash flow: EUR %.2f
                Spending by category: %s
                Income by category: %s
                Goal progress: %s
                Account balances: %s

                Write a short, friendly weekly summary (3-5 sentences). Include:
                - Where most money went
                - One practical tip based on the data
                - Progress toward any active goals
                - Any notable investment account performance if applicable
                """.formatted(
                snapshot.weekStart(),
                snapshot.weekEnd(),
                snapshot.totalSpent().doubleValue(),
                snapshot.totalIncome().doubleValue(),
                snapshot.netCashFlow().doubleValue(),
                snapshot.spendingByCategory(),
                snapshot.incomeByCategory(),
                snapshot.goalProgress(),
                snapshot.accountSummaries());

            log.info("Calling LLM...");
            ChatClient client = llmProviderRouter.getClientForTask(LlmProviderRouter.TaskType.SUMMARY);
            String summaryText = client.prompt()
                .user(prompt)
                .call()
                .content();
            log.info("LLM response received, length={}", summaryText != null ? summaryText.length() : 0);

            WeeklySummary summary = new WeeklySummary();
            summary.setWeekStart(snapshot.weekStart());
            summary.setWeekEnd(snapshot.weekEnd());
            summary.setTotalSpent(snapshot.totalSpent());
            summary.setSummaryText(summaryText != null ? summaryText : "No summary generated.");
            summary.setProvider(config.getSummaryProvider());

            WeeklySummary saved = weeklySummaryRepository.save(summary);
            log.info("Weekly summary saved with id={}", saved.getId());
            return toResponse(saved);
        } catch (Exception e) {
            log.error("Weekly summary generation failed: {}", e.getMessage(), e);
            throw e;
        }
        }

        private WeeklyDataSnapshot buildSnapshot() {
        LocalDate weekEnd = LocalDate.now();
        LocalDate weekStart = weekEnd.minusDays(6);

        BigDecimal totalSpent = expenseRepository.findByWeek(weekStart, weekEnd).stream()
            .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Income> incomesForMonth = incomeRepository.findByFilters(YearMonth.now().format(MONTH_FMT), null);
        BigDecimal totalIncome = incomesForMonth.stream()
            .filter(i -> i.getIncomeDate() != null
                && !i.getIncomeDate().isBefore(weekStart)
                && !i.getIncomeDate().isAfter(weekEnd))
            .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String spendingByCategory = expenseRepository.findByWeek(weekStart, weekEnd).stream()
            .collect(Collectors.groupingBy(
                e -> e.getCategory() != null ? e.getCategory().getName() : "Other",
                Collectors.mapping(
                    e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
            .toString();

        String incomeByCategory = incomesForMonth.stream()
            .filter(i -> i.getIncomeDate() != null
                && !i.getIncomeDate().isBefore(weekStart)
                && !i.getIncomeDate().isAfter(weekEnd))
            .collect(Collectors.groupingBy(
                i -> i.getIncomeCategory() != null ? i.getIncomeCategory().getName() : "Other",
                Collectors.mapping(
                    i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
            .toString();

        String goalProgress = goalService.findAllActive().toString();
        String accountSummaries = accountService.getAllAccounts().toString();

        return new WeeklyDataSnapshot(
            weekStart,
            weekEnd,
            totalSpent != null ? totalSpent : BigDecimal.ZERO,
            totalIncome != null ? totalIncome : BigDecimal.ZERO,
            (totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .subtract(totalSpent != null ? totalSpent : BigDecimal.ZERO),
            spendingByCategory != null ? spendingByCategory : "{}",
            incomeByCategory != null ? incomeByCategory : "{}",
            goalProgress != null ? goalProgress : "[]",
            accountSummaries != null ? accountSummaries : "[]");
        }

        private record WeeklyDataSnapshot(
            LocalDate weekStart,
            LocalDate weekEnd,
            BigDecimal totalSpent,
            BigDecimal totalIncome,
            BigDecimal netCashFlow,
            String spendingByCategory,
            String incomeByCategory,
            String goalProgress,
            String accountSummaries
        ) {
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
