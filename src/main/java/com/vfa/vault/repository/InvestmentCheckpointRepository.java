package com.vfa.vault.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.InvestmentCheckpoint;
import com.vfa.vault.repository.projection.AccountIdAmountProjection;

@Repository
public interface InvestmentCheckpointRepository extends JpaRepository<InvestmentCheckpoint, UUID> {

    List<InvestmentCheckpoint> findByAccountIdOrderByRecordedAtDesc(UUID accountId);

    Optional<InvestmentCheckpoint> findTopByAccountIdOrderByRecordedAtDesc(UUID accountId);

    @Query(value = """
            SELECT DISTINCT ON (c.account_id) c.account_id AS accountId, c.value AS total
            FROM investment_checkpoints c
            WHERE c.account_id IN (:accountIds)
            ORDER BY c.account_id, c.recorded_at DESC
            """, nativeQuery = true)
    List<AccountIdAmountProjection> findLatestValuesByAccountIds(
            @Param("accountIds") Collection<UUID> accountIds);
}
