package com.vfa.vault.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.Transfer;
import com.vfa.vault.repository.projection.AccountIdAmountProjection;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    boolean existsByOriginalTransferId(UUID originalTransferId);

    @EntityGraph(attributePaths = {"fromAccount", "toAccount"})
    @Query("""
            SELECT t FROM Transfer t
            WHERE t.fromAccount.id = :accountId
               OR t.toAccount.id = :accountId
            ORDER BY t.transferDate DESC, t.createdAt DESC
            """)
    List<Transfer> findByAccountId(@Param("accountId") UUID accountId);

    @Override
    @EntityGraph(attributePaths = {"fromAccount", "toAccount", "originalTransfer"})
    Optional<Transfer> findById(UUID id);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transfer t
            WHERE t.fromAccount.id = :accountId
              AND t.isReversal = false
              AND NOT EXISTS (
                  SELECT 1 FROM Transfer r
                  WHERE r.originalTransfer.id = t.id
              )
            """)
    BigDecimal sumOutgoingByAccountId(@Param("accountId") UUID accountId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transfer t
            WHERE t.toAccount.id = :accountId
              AND t.isReversal = false
              AND NOT EXISTS (
                  SELECT 1 FROM Transfer r
                  WHERE r.originalTransfer.id = t.id
              )
            """)
    BigDecimal sumIncomingByAccountId(@Param("accountId") UUID accountId);

    @Query("""
            SELECT t.fromAccount.id AS accountId, COALESCE(SUM(t.amount), 0) AS total
            FROM Transfer t
            WHERE t.fromAccount.id IN :accountIds
              AND t.isReversal = false
              AND NOT EXISTS (
                  SELECT 1 FROM Transfer r
                  WHERE r.originalTransfer.id = t.id
              )
            GROUP BY t.fromAccount.id
            """)
    List<AccountIdAmountProjection> sumOutgoingByAccountIds(@Param("accountIds") Collection<UUID> accountIds);

    @Query("""
            SELECT t.toAccount.id AS accountId, COALESCE(SUM(t.amount), 0) AS total
            FROM Transfer t
            WHERE t.toAccount.id IN :accountIds
              AND t.isReversal = false
              AND NOT EXISTS (
                  SELECT 1 FROM Transfer r
                  WHERE r.originalTransfer.id = t.id
              )
            GROUP BY t.toAccount.id
            """)
    List<AccountIdAmountProjection> sumIncomingByAccountIds(@Param("accountIds") Collection<UUID> accountIds);
}
