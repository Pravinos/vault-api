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

import com.vfa.vault.entity.Income;

@Repository
public interface IncomeRepository extends JpaRepository<Income, UUID> {

    List<Income> findByAccountId(UUID accountId);

    @Query("""
            SELECT i FROM Income i
            WHERE (:month IS NULL OR FUNCTION('TO_CHAR', i.incomeDate, 'YYYY-MM') = :month)
            AND (:accountId IS NULL OR i.account.id = :accountId)
            ORDER BY i.incomeDate DESC
            """)
    List<Income> findByFilters(@Param("month") String month, @Param("accountId") UUID accountId);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.account.id = :accountId")
    BigDecimal sumByAccountId(@Param("accountId") UUID accountId);

    @Query("""
        SELECT COALESCE(SUM(i.amount), 0) FROM Income i
        WHERE YEAR(i.incomeDate) = :year AND MONTH(i.incomeDate) = :month
        """)
    Optional<BigDecimal> sumByYearMonth(@Param("year") int year, @Param("month") int month);

        @Query("""
            SELECT ic.name, SUM(i.amount)
            FROM Income i JOIN i.incomeCategory ic
            WHERE i.incomeDate BETWEEN :start AND :end
            GROUP BY ic.name
            """)
        List<Object[]> sumByCategoryBetweenDatesRaw(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
