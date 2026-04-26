package com.vfa.vault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vfa.vault.dto.CategoryDTO;
import com.vfa.vault.dto.ExpenseDTO;
import com.vfa.vault.entity.Expense;
import com.vfa.vault.exception.ResourceNotFoundException;
import com.vfa.vault.repository.CategoryRepository;
import com.vfa.vault.repository.ExpenseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public List<ExpenseDTO.Response> findAll(String month, Integer categoryId) {
        List<Expense> expenses;

        if (month != null && categoryId != null) {
            YearMonth ym = YearMonth.parse(month, MONTH_FMT);
            expenses = expenseRepository
                    .findByExpenseDateBetweenAndCategoryIdOrderByExpenseDateDesc(
                            ym.atDay(1), ym.atEndOfMonth(), categoryId);
        } else if (month != null) {
            expenses = expenseRepository.findByMonth(month);
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

        var expense = new Expense();
        expense.setAmount(request.amount());
        expense.setNote(request.note());
        expense.setCategory(category);
        expense.setExpenseDate(
                request.expenseDate() != null ? request.expenseDate() : LocalDate.now());

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseDTO.Response update(UUID id, ExpenseDTO.Request request) {
        var expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        expense.setAmount(request.amount());
        expense.setNote(request.note());
        expense.setCategory(category);
        if (request.expenseDate() != null) expense.setExpenseDate(request.expenseDate());

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(UUID id) {
        if (!expenseRepository.existsById(id))
            throw new ResourceNotFoundException("Expense", id);
        expenseRepository.deleteById(id);
    }

    public ExpenseDTO.MonthlySummary getMonthlySummary(String month) {
        if (month == null) month = YearMonth.now().format(MONTH_FMT);

        BigDecimal total = expenseRepository.totalForMonth(month);
        if (total == null) total = BigDecimal.ZERO;

        List<Object[]> rows = expenseRepository.sumByCategoryForMonth(month);
        List<ExpenseDTO.CategoryTotal> byCategory = rows.stream()
                .map(r -> new ExpenseDTO.CategoryTotal(
                        (String) r[0],
                        (BigDecimal) r[1]))
                .toList();

        return new ExpenseDTO.MonthlySummary(month, total, byCategory);
    }

    public ExpenseDTO.Stats getStats() {
        String thisMonth = YearMonth.now().format(MONTH_FMT);
        String lastMonth = YearMonth.now().minusMonths(1).format(MONTH_FMT);

        BigDecimal totalThis = expenseRepository.totalForMonth(thisMonth);
        if (totalThis == null) totalThis = BigDecimal.ZERO;

        BigDecimal totalLast = expenseRepository.totalForMonth(lastMonth);
        if (totalLast == null) totalLast = BigDecimal.ZERO;

        int daysThisMonth = YearMonth.now().lengthOfMonth();
        BigDecimal avgPerDay = totalThis.divide(
                BigDecimal.valueOf(daysThisMonth), 2, RoundingMode.HALF_UP);

        List<Object[]> byCat = expenseRepository.sumByCategoryForMonth(thisMonth);
        String topCategory = byCat.isEmpty() ? "N/A" : (String) byCat.get(0)[0];

        long count = expenseRepository.findByMonth(thisMonth).size();

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
                e.getCreatedAt());
    }
}