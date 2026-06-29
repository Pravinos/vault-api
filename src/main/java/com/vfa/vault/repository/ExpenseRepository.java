package com.vfa.vault.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.Expense;
import com.vfa.vault.repository.projection.AccountIdAmountProjection;
import com.vfa.vault.repository.projection.CategoryAmountProjection;
import com.vfa.vault.repository.projection.CategoryIdAmountProjection;
import com.vfa.vault.repository.projection.DayAmountProjection;
import com.vfa.vault.repository.projection.ExpenseDateAmountProjection;
import com.vfa.vault.repository.projection.MonthAmountProjection;
import com.vfa.vault.repository.projection.TopCategoryProjection;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @EntityGraph(attributePaths = {"category", "account"})
    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDesc(
            LocalDate start, LocalDate end);

    long countByExpenseDateBetween(LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"category", "account"})
    List<Expense> findByCategoryIdOrderByExpenseDateDesc(Integer categoryId);

    @EntityGraph(attributePaths = {"category", "account"})
    List<Expense> findByExpenseDateBetweenAndCategoryIdOrderByExpenseDateDesc(
            LocalDate start, LocalDate end, Integer categoryId);

    @Override
    @EntityGraph(attributePaths = {"category", "account"})
    List<Expense> findAll();

    @EntityGraph(attributePaths = {"category", "account"})
    @Query("""
            SELECT e FROM Expense e
            WHERE e.expenseDate >= :weekStart
            AND e.expenseDate <= :weekEnd
            ORDER BY e.expenseDate DESC
            """)
    List<Expense> findByWeek(
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd);

    @Query("""
            SELECT c.name AS name, SUM(e.amount) AS total
            FROM Expense e JOIN e.category c
            WHERE e.expenseDate >= :start AND e.expenseDate <= :end
            GROUP BY c.name
            ORDER BY total DESC
            """)
    List<CategoryAmountProjection> sumByCategoryForDateRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT SUM(e.amount)
            FROM Expense e
            WHERE e.expenseDate >= :start AND e.expenseDate <= :end
            """)
    BigDecimal totalForDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT CAST(e.expenseDate AS string) AS day, SUM(e.amount) AS total
            FROM Expense e
            WHERE e.expenseDate >= :since
            GROUP BY e.expenseDate
            ORDER BY e.expenseDate DESC
            """)
    List<DayAmountProjection> dailyTotalsFrom(@Param("since") LocalDate since);

    @Query("""
            SELECT FUNCTION('TO_CHAR', e.expenseDate, 'YYYY-MM') AS month,
                   SUM(e.amount) AS total
            FROM Expense e
            WHERE e.category.name = :category
            AND e.expenseDate >= :since
            GROUP BY FUNCTION('TO_CHAR', e.expenseDate, 'YYYY-MM')
            ORDER BY month DESC
            """)
    List<MonthAmountProjection> monthlyTotalsByCategory(
            @Param("category") String category,
            @Param("since") LocalDate since);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.account.id = :accountId")
    BigDecimal sumByAccountId(@Param("accountId") UUID accountId);

    @Query("""
            SELECT e.account.id AS accountId, COALESCE(SUM(e.amount), 0) AS total
            FROM Expense e
            WHERE e.account.id IN :accountIds
            GROUP BY e.account.id
            """)
    List<AccountIdAmountProjection> sumByAccountIds(@Param("accountIds") Collection<UUID> accountIds);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
            WHERE e.expenseDate >= :start AND e.expenseDate <= :end
            """)
    Optional<BigDecimal> sumByYearMonth(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT c.name AS categoryName, SUM(e.amount) AS total
            FROM Expense e JOIN e.category c
            WHERE e.expenseDate >= :start AND e.expenseDate <= :end
            GROUP BY c.id, c.name
            ORDER BY SUM(e.amount) DESC
            LIMIT 1
            """)
    Optional<TopCategoryProjection> findTopCategoryForDateRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT c.name AS name, SUM(e.amount) AS total
            FROM Expense e JOIN e.category c
            WHERE e.expenseDate BETWEEN :start AND :end
            GROUP BY c.name
            """)
    List<CategoryAmountProjection> sumByCategoryBetweenDates(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT e.category.id AS categoryId, SUM(e.amount) AS total
            FROM Expense e
            WHERE e.expenseDate >= :start AND e.expenseDate < :end
            GROUP BY e.category.id
            """)
    List<CategoryIdAmountProjection> sumByCategoryIdForDateRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT e.expenseDate AS expenseDate, SUM(e.amount) AS total
            FROM Expense e
            WHERE e.expenseDate >= :start AND e.expenseDate <= :end
            GROUP BY e.expenseDate
            ORDER BY e.expenseDate
            """)
    List<ExpenseDateAmountProjection> sumByDayForYear(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
