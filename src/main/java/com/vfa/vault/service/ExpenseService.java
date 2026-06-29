package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.CategoryDTO;
import com.vfa.vault.dto.ExpenseDTO;
import com.vfa.vault.dto.ExpenseHeatmapDTO;
import com.vfa.vault.entity.Expense;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.AccountRepository;
import com.vfa.vault.repository.CategoryRepository;
import com.vfa.vault.repository.ExpenseRepository;
import com.vfa.vault.repository.projection.ExpenseDateAmountProjection;
import com.vfa.vault.util.MonthParser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<ExpenseDTO.Response> findAll(String month, Integer categoryId) {
        List<Expense> expenses;

        if (month != null && categoryId != null) {
            var ym = MonthParser.parseYearMonth(month);
            expenses = expenseRepository
                    .findByExpenseDateBetweenAndCategoryIdOrderByExpenseDateDesc(
                            ym.atDay(1), ym.atEndOfMonth(), categoryId);
        } else if (month != null) {
            var ym = MonthParser.parseYearMonth(month);
            expenses = expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(
                    ym.atDay(1), ym.atEndOfMonth());
        } else if (categoryId != null) {
            expenses = expenseRepository.findByCategoryIdOrderByExpenseDateDesc(categoryId);
        } else {
            expenses = expenseRepository.findAll();
        }

        return expenses.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ExpenseDTO.Response create(ExpenseDTO.Request request) {
        var category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        var account = accountRepository.findById(request.accountId())
            .orElseThrow(() -> new ResourceNotFoundException("Account", request.accountId()));

        var expense = new Expense();
        expense.setAmount(request.amount());
        expense.setNote(request.note());
        expense.setCategory(category);
        expense.setAccount(account);
        expense.setExpenseDate(
                request.expenseDate() != null ? request.expenseDate() : LocalDate.now());

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseDTO.Response update(UUID id, ExpenseDTO.Request request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        var account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.accountId()));
        expense.setAmount(request.amount());
        expense.setNote(request.note());
        expense.setCategory(category);
        expense.setAccount(account);
        if (request.expenseDate() != null) {
            expense.setExpenseDate(request.expenseDate());
        }
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(UUID id) {
        // Hard delete only. Idempotent by design: deleting a missing row is a no-op.
        expenseRepository.findById(id).ifPresent(expense -> {
            expenseRepository.delete(expense);
            expenseRepository.flush();
        });
    }

    @Transactional(readOnly = true)
    public ExpenseDTO.MonthlySummary getMonthlySummary(String month) {
        if (month == null) {
            month = MonthParser.currentMonth();
        }

        var ym = MonthParser.parseYearMonth(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal total = expenseRepository.totalForDateRange(start, end);
        if (total == null) total = BigDecimal.ZERO;

        List<ExpenseDTO.CategoryTotal> byCategory = expenseRepository
                .sumByCategoryForDateRange(start, end)
                .stream()
                .map(r -> new ExpenseDTO.CategoryTotal(r.getName(), r.getTotal()))
                .toList();

        return new ExpenseDTO.MonthlySummary(month, total, byCategory);
    }

    @Transactional(readOnly = true)
    public ExpenseHeatmapDTO getHeatmap(int year) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("year must be between 2000 and 2100");
        }
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<ExpenseDateAmountProjection> rows = expenseRepository.sumByDayForYear(start, end);
        Map<LocalDate, BigDecimal> byDay = rows.stream()
                .collect(Collectors.toMap(
                        ExpenseDateAmountProjection::getExpenseDate,
                        ExpenseDateAmountProjection::getTotal));
        List<ExpenseHeatmapDTO.DayTotal> days = new ArrayList<>();
        BigDecimal max = BigDecimal.ZERO;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            BigDecimal amount = byDay.getOrDefault(d, BigDecimal.ZERO);
            if (amount.compareTo(max) > 0) max = amount;
            days.add(new ExpenseHeatmapDTO.DayTotal(d.toString(), amount));
        }
        return new ExpenseHeatmapDTO(year, days, max);
    }

    @Transactional(readOnly = true)
    public ExpenseDTO.Stats getStats() {
        var thisYm = MonthParser.parseYearMonth(MonthParser.currentMonth());
        var lastYm = thisYm.minusMonths(1);

        BigDecimal totalThis = expenseRepository.totalForDateRange(
                thisYm.atDay(1), thisYm.atEndOfMonth());
        if (totalThis == null) totalThis = BigDecimal.ZERO;

        BigDecimal totalLast = expenseRepository.totalForDateRange(
                lastYm.atDay(1), lastYm.atEndOfMonth());
        if (totalLast == null) totalLast = BigDecimal.ZERO;

        int daysThisMonth = thisYm.lengthOfMonth();
        BigDecimal avgPerDay = totalThis.divide(
                BigDecimal.valueOf(daysThisMonth), 2, RoundingMode.HALF_UP);

        List<ExpenseDTO.CategoryTotal> byCat = expenseRepository
                .sumByCategoryForDateRange(thisYm.atDay(1), thisYm.atEndOfMonth())
                .stream()
                .map(r -> new ExpenseDTO.CategoryTotal(r.getName(), r.getTotal()))
                .toList();
        String topCategory = byCat.isEmpty() ? "N/A" : byCat.get(0).category();

        long count = expenseRepository.countByExpenseDateBetween(
                thisYm.atDay(1), thisYm.atEndOfMonth());

        return new ExpenseDTO.Stats(totalThis, totalLast, avgPerDay, topCategory, count);
    }

    private ExpenseDTO.Response toResponse(Expense e) {
        return new ExpenseDTO.Response(
                e.getId(),
                e.getAmount(),
                e.getNote(),
                new CategoryDTO.Response(
                        e.getCategory().getId(),
                        e.getCategory().getName(),
                        e.getCategory().getIcon()),
                e.getExpenseDate(),
                e.getCreatedAt(),
                e.getAccount() != null ? e.getAccount().getId() : null,
                e.getAccount() != null ? e.getAccount().getName() : null);
    }
}