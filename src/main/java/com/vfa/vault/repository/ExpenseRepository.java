package com.vfa.vault.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDesc(
            LocalDate start, LocalDate end);

    List<Expense> findByCategoryIdOrderByExpenseDateDesc(Integer categoryId);

    List<Expense> findByExpenseDateBetweenAndCategoryIdOrderByExpenseDateDesc(
            LocalDate start, LocalDate end, Integer categoryId);

    @Query("""
            SELECT e FROM Expense e
            WHERE FUNCTION('TO_CHAR', e.expenseDate, 'YYYY-MM') = :month
            ORDER BY e.expenseDate DESC
            """)
    List<Expense> findByMonth(@Param("month") String month);

    @Query("""
            SELECT c.name AS category, SUM(e.amount) AS total
            FROM Expense e JOIN e.category c
            WHERE FUNCTION('TO_CHAR', e.expenseDate, 'YYYY-MM') = :month
            GROUP BY c.name
            ORDER BY total DESC
            """)
    List<Object[]> sumByCategoryForMonth(@Param("month") String month);

    @Query("""
            SELECT SUM(e.amount)
            FROM Expense e
            WHERE FUNCTION('TO_CHAR', e.expenseDate, 'YYYY-MM') = :month
            """)
    BigDecimal totalForMonth(@Param("month") String month);

    @Query("""
            SELECT SUM(e.amount)
            FROM Expense e
            WHERE FUNCTION('TO_CHAR', e.expenseDate, 'YYYY-MM') = :month
            AND e.category.id = :categoryId
            """)
    BigDecimal totalForMonthAndCategory(
            @Param("month") String month,
            @Param("categoryId") Integer categoryId);

    @Query("""
            SELECT CAST(e.expenseDate AS string) AS day, SUM(e.amount) AS total
            FROM Expense e
            WHERE e.expenseDate >= :since
            GROUP BY e.expenseDate
            ORDER BY e.expenseDate DESC
            """)
    List<Object[]> dailyTotalsFrom(@Param("since") LocalDate since);

    @Query("""
            SELECT FUNCTION('TO_CHAR', e.expenseDate, 'YYYY-MM') AS month,
                   SUM(e.amount) AS total
            FROM Expense e
            WHERE e.category.name = :category
            AND e.expenseDate >= :since
            GROUP BY FUNCTION('TO_CHAR', e.expenseDate, 'YYYY-MM')
            ORDER BY month DESC
            """)
    List<Object[]> monthlyTotalsByCategory(
            @Param("category") String category,
            @Param("since") LocalDate since);

    @Query("""
            SELECT e FROM Expense e
            WHERE e.expenseDate >= :weekStart
            AND e.expenseDate <= :weekEnd
            ORDER BY e.expenseDate DESC
            """)
    List<Expense> findByWeek(
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd);
}