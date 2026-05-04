package com.vfa.vault.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.Transfer;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    boolean existsByOriginalTransferId(UUID originalTransferId);

    @Query("""
            SELECT t FROM Transfer t
            WHERE t.fromAccount.id = :accountId
               OR t.toAccount.id = :accountId
            ORDER BY t.transferDate DESC, t.createdAt DESC
            """)
    List<Transfer> findByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT SUM(t.amount) FROM Transfer t WHERE t.fromAccount.id = :accountId")
    BigDecimal sumOutgoingByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT SUM(t.amount) FROM Transfer t WHERE t.toAccount.id = :accountId")
    BigDecimal sumIncomingByAccountId(@Param("accountId") UUID accountId);
}
