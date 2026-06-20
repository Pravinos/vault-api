package com.vfa.vault.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.Budget;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByMonth(LocalDate month);

    Optional<Budget> findByCategoryIdAndMonth(Integer categoryId, LocalDate month);
}
