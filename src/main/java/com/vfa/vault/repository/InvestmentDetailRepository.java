package com.vfa.vault.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.InvestmentDetail;

@Repository
public interface InvestmentDetailRepository extends JpaRepository<InvestmentDetail, UUID> {

    Optional<InvestmentDetail> findByAccountId(UUID accountId);

    List<InvestmentDetail> findByAccountIdIn(Collection<UUID> accountIds);

    void deleteByAccountId(UUID accountId);
}
