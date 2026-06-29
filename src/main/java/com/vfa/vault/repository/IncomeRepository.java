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

import com.vfa.vault.entity.Income;
import com.vfa.vault.repository.projection.AccountIdAmountProjection;
import com.vfa.vault.repository.projection.CategoryAmountProjection;

@Repository
public interface IncomeRepository extends JpaRepository<Income, UUID> {

    @EntityGraph(attributePaths = {"incomeCategory", "account"})
    List<Income> findByIncomeDateBetweenOrderByIncomeDateDesc(LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"incomeCategory", "account"})
    List<Income> findByIncomeDateBetweenAndAccountIdOrderByIncomeDateDesc(
            LocalDate start, LocalDate end, UUID accountId);

    @EntityGraph(attributePaths = {"incomeCategory", "account"})
    List<Income> findByAccountIdOrderByIncomeDateDesc(UUID accountId);

    @Override
    @EntityGraph(attributePaths = {"incomeCategory", "account"})
    List<Income> findAll();

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.account.id = :accountId")
    BigDecimal sumByAccountId(@Param("accountId") UUID accountId);

    @Query("""
            SELECT i.account.id AS accountId, COALESCE(SUM(i.amount), 0) AS total
            FROM Income i
            WHERE i.account.id IN :accountIds
            GROUP BY i.account.id
            """)
    List<AccountIdAmountProjection> sumByAccountIds(@Param("accountIds") Collection<UUID> accountIds);

    @Query("""
            SELECT COALESCE(SUM(i.amount), 0) FROM Income i
            WHERE i.incomeDate >= :start AND i.incomeDate <= :end
            """)
    Optional<BigDecimal> sumByYearMonth(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT ic.name AS name, SUM(i.amount) AS total
            FROM Income i JOIN i.incomeCategory ic
            WHERE i.incomeDate BETWEEN :start AND :end
            GROUP BY ic.name
            """)
    List<CategoryAmountProjection> sumByCategoryBetweenDates(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT ic.name AS name, SUM(i.amount) AS total
            FROM Income i JOIN i.incomeCategory ic
            WHERE i.incomeDate >= :start AND i.incomeDate <= :end
            GROUP BY ic.name
            ORDER BY SUM(i.amount) DESC
            """)
    List<CategoryAmountProjection> sumByCategoryForDateRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
