package com.vfa.vault.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.WeeklySummary;

@Repository
public interface WeeklySummaryRepository extends JpaRepository<WeeklySummary, UUID> {

    List<WeeklySummary> findAllByOrderByGeneratedAtDesc();

    Optional<WeeklySummary> findTopByOrderByGeneratedAtDesc();
}