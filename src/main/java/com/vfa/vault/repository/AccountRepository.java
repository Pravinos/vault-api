package com.vfa.vault.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByIsActiveTrue();

    Optional<Account> findByIdAndIsActiveTrue(UUID id);

    @Query("""
        SELECT a FROM Account a
        WHERE a.isActive = true
        ORDER BY
            GREATEST(a.createdAt,
                     COALESCE(a.manualBalanceUpdatedAt, a.createdAt)) DESC
        """)
    List<Account> findAllActiveOrderByLastUpdatedDesc();
}
