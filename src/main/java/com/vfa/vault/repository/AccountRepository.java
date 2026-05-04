package com.vfa.vault.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Query("""
        SELECT a FROM Account a
        ORDER BY
            GREATEST(a.createdAt,
                     COALESCE(a.manualBalanceUpdatedAt, a.createdAt)) DESC
        """)
    List<Account> findAllOrderByLastUpdatedDesc();
}
