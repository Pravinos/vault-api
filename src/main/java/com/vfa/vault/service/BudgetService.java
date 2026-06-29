package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.BudgetDTO;
import com.vfa.vault.dto.BudgetStatus;
import com.vfa.vault.dto.BudgetSummaryDTO;
import com.vfa.vault.entity.Budget;
import com.vfa.vault.entity.Category;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.BudgetRepository;
import com.vfa.vault.repository.CategoryRepository;
import com.vfa.vault.repository.ExpenseRepository;
import com.vfa.vault.repository.projection.CategoryIdAmountProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.80");

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<BudgetDTO.Response> getBudgetsForMonth(String month) {
        LocalDate monthStart = parseMonth(month);
        return budgetRepository.findByMonth(monthStart).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public BudgetDTO.Response upsertBudget(BudgetDTO.Request request) {
        LocalDate monthStart = parseMonth(request.month());
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        Budget budget = budgetRepository.findByCategoryIdAndMonth(request.categoryId(), monthStart)
                .orElseGet(Budget::new);
        budget.setCategory(category);
        budget.setMonth(monthStart);
        budget.setAmount(request.amount());

        return toDto(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(UUID id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetSummaryDTO> getBudgetSummary(String month) {
        LocalDate monthStart = parseMonth(month);
        LocalDate monthEnd = monthStart.plusMonths(1);

        List<Budget> budgets = budgetRepository.findByMonth(monthStart);
        Map<Integer, BigDecimal> spentByCategory = loadSpentByCategory(monthStart, monthEnd);

        return budgets.stream()
                .map(budget -> toSummary(budget, spentByCategory.getOrDefault(
                        budget.getCategory().getId(), BigDecimal.ZERO)))
                .sorted(Comparator.comparingDouble(BudgetSummaryDTO::percentageUsed).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BudgetSummaryDTO> getBudgetAlerts(String month) {
        return getBudgetSummary(month).stream()
                .filter(b -> BudgetStatus.WARNING.name().equals(b.status())
                        || BudgetStatus.OVER_BUDGET.name().equals(b.status()))
                .toList();
    }

    private Map<Integer, BigDecimal> loadSpentByCategory(LocalDate monthStart, LocalDate monthEnd) {
        Map<Integer, BigDecimal> spentByCategory = new HashMap<>();
        for (CategoryIdAmountProjection row : expenseRepository.sumByCategoryIdForDateRange(monthStart, monthEnd)) {
            Integer categoryId = row.getCategoryId();
            if (categoryId != null) {
                spentByCategory.put(categoryId, row.getTotal());
            }
        }
        return spentByCategory;
    }

    private BudgetSummaryDTO toSummary(Budget budget, BigDecimal spentAmount) {
        Category category = budget.getCategory();
        BigDecimal budgetAmount = budget.getAmount();
        BigDecimal remainingAmount = budgetAmount.subtract(spentAmount);
        double percentageUsed = calculatePercentageUsed(budgetAmount, spentAmount);
        String status = resolveStatus(budgetAmount, spentAmount).name();

        return new BudgetSummaryDTO(
                category.getId(),
                category.getName(),
                category.getIcon(),
                budgetAmount,
                spentAmount,
                remainingAmount,
                percentageUsed,
                status);
    }

    static BudgetStatus resolveStatus(BigDecimal budgetAmount, BigDecimal spentAmount) {
        if (spentAmount.compareTo(budgetAmount) >= 0) {
            return BudgetStatus.OVER_BUDGET;
        }
        BigDecimal warningThreshold = budgetAmount.multiply(WARNING_THRESHOLD);
        if (spentAmount.compareTo(warningThreshold) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.ON_TRACK;
    }

    private double calculatePercentageUsed(BigDecimal budgetAmount, BigDecimal spentAmount) {
        if (budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return spentAmount
                .divide(budgetAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private BudgetDTO.Response toDto(Budget budget) {
        Category category = budget.getCategory();
        return new BudgetDTO.Response(
                budget.getId(),
                category.getId(),
                category.getName(),
                category.getIcon(),
                formatMonth(budget.getMonth()),
                budget.getAmount());
    }

    private LocalDate parseMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException("Month must be in YYYY-MM format");
        }
        try {
            return YearMonth.parse(month.trim(), MONTH_FMT).atDay(1);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Month must be in YYYY-MM format");
        }
    }

    private String formatMonth(LocalDate month) {
        return YearMonth.from(month).format(MONTH_FMT);
    }
}
