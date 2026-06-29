package com.vfa.vault.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.InvestmentCheckpoint;

@Repository
public interface InvestmentCheckpointRepository extends JpaRepository<InvestmentCheckpoint, UUID> {

    List<InvestmentCheckpoint> findByAccountIdOrderByRecordedAtDesc(UUID accountId);

    Optional<InvestmentCheckpoint> findTopByAccountIdOrderByRecordedAtDesc(UUID accountId);
}
