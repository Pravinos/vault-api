package com.vfa.vault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.IncomeCategory;

@Repository
public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, Integer> {
}
