package com.vfa.vault.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void getBudgetsForMonth_rejectsInvalidMonth() {
        assertThatThrownBy(() -> budgetService.getBudgetsForMonth("not-a-month"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Month must be in YYYY-MM format");
    }

    @Test
    void upsertBudget_createsNewBudget() {
        Category category = category(1, "Groceries", "🛒");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(budgetRepository.findByCategoryIdAndMonth(1, LocalDate.of(2026, 6, 1)))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget saved = invocation.getArgument(0);
            saved.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return saved;
        });

        BudgetDTO.Response response = budgetService.upsertBudget(
                new BudgetDTO.Request(1, "2026-06", new BigDecimal("500.00")));

        assertThat(response.id()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(response.categoryId()).isEqualTo(1);
        assertThat(response.categoryName()).isEqualTo("Groceries");
        assertThat(response.month()).isEqualTo("2026-06");
        assertThat(response.amount()).isEqualByComparingTo("500.00");

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(captor.capture());
        assertThat(captor.getValue().getMonth()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void upsertBudget_throwsWhenCategoryMissing() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.upsertBudget(
                new BudgetDTO.Request(99, "2026-06", new BigDecimal("100.00"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBudgetSummary_assignsWarningAtEightyPercent() {
        Category category = category(1, "Groceries", "🛒");
        Budget budget = budget(category, new BigDecimal("100.00"), LocalDate.of(2026, 6, 1));
        when(budgetRepository.findByMonth(LocalDate.of(2026, 6, 1))).thenReturn(List.of(budget));
        when(expenseRepository.sumByCategoryIdForDateRange(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(categoryRow(1, new BigDecimal("80.00"))));

        List<BudgetSummaryDTO> summary = budgetService.getBudgetSummary("2026-06");

        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).status()).isEqualTo(BudgetStatus.WARNING);
        assertThat(summary.get(0).percentageUsed()).isEqualTo(80.0);
    }

    @Test
    void resolveStatus_usesBudgetComparisonsAtBoundaries() {
        BigDecimal budget = new BigDecimal("100.00");

        assertThat(BudgetService.resolveStatus(budget, new BigDecimal("79.99")))
                .isEqualTo(BudgetStatus.ON_TRACK);
        assertThat(BudgetService.resolveStatus(budget, new BigDecimal("80.00")))
                .isEqualTo(BudgetStatus.WARNING);
        assertThat(BudgetService.resolveStatus(budget, new BigDecimal("99.99")))
                .isEqualTo(BudgetStatus.WARNING);
        assertThat(BudgetService.resolveStatus(budget, new BigDecimal("100.00")))
                .isEqualTo(BudgetStatus.OVER_BUDGET);
        assertThat(BudgetService.resolveStatus(budget, new BigDecimal("150.00")))
                .isEqualTo(BudgetStatus.OVER_BUDGET);
    }

    @Test
    void getBudgetAlerts_returnsOnlyWarningAndOverBudget() {
        Category onTrackCategory = category(1, "Groceries", "🛒");
        Category warningCategory = category(2, "Dining", "🍽️");
        Category overCategory = category(3, "Travel", "✈️");

        when(budgetRepository.findByMonth(LocalDate.of(2026, 6, 1))).thenReturn(List.of(
                budget(onTrackCategory, new BigDecimal("100.00"), LocalDate.of(2026, 6, 1)),
                budget(warningCategory, new BigDecimal("100.00"), LocalDate.of(2026, 6, 1)),
                budget(overCategory, new BigDecimal("100.00"), LocalDate.of(2026, 6, 1))));
        when(expenseRepository.sumByCategoryIdForDateRange(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(
                        categoryRow(1, new BigDecimal("50.00")),
                        categoryRow(2, new BigDecimal("80.00")),
                        categoryRow(3, new BigDecimal("100.00"))));

        List<BudgetSummaryDTO> alerts = budgetService.getBudgetAlerts("2026-06");

        assertThat(alerts).extracting(BudgetSummaryDTO::status)
                .containsExactly(BudgetStatus.OVER_BUDGET, BudgetStatus.WARNING);
        assertThat(alerts).extracting(BudgetSummaryDTO::categoryName)
                .containsExactly("Travel", "Dining");
    }

    private static Category category(int id, String name, String icon) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setIcon(icon);
        return category;
    }

    private static Budget budget(Category category, BigDecimal amount, LocalDate month) {
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setCategory(category);
        budget.setAmount(amount);
        budget.setMonth(month);
        return budget;
    }

    private static CategoryIdAmountProjection categoryRow(int categoryId, BigDecimal amount) {
        return new CategoryIdAmountProjection() {
            @Override
            public Integer getCategoryId() {
                return categoryId;
            }

            @Override
            public BigDecimal getTotal() {
                return amount;
            }
        };
    }
}
