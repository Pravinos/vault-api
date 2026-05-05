package com.vfa.vault.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.account.id = :accountId")
    BigDecimal sumByAccountId(@Param("accountId") UUID accountId);

        @Query("""
                SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
                WHERE YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month
                """)
        Optional<BigDecimal> sumByYearMonth(@Param("year") int year, @Param("month") int month);

        @Query(value = """
                SELECT c.name AS category_name, SUM(e.amount) AS total
                FROM expenses e
                JOIN categories c ON c.id = e.category_id
                WHERE EXTRACT(YEAR FROM e.expense_date) = :year
                  AND EXTRACT(MONTH FROM e.expense_date) = :month
                GROUP BY c.id, c.name
                ORDER BY total DESC
                LIMIT 1
                """, nativeQuery = true)
        Optional<Object[]> findTopCategoryByYearMonth(@Param("year") int year, @Param("month") int month);

    @Query("""
            SELECT c.name, SUM(e.amount)
            FROM Expense e JOIN e.category c
            WHERE e.expenseDate BETWEEN :start AND :end
            GROUP BY c.name
            """)
    List<Object[]> sumByCategoryBetweenDatesRaw(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}