package com.vfa.vault.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.Goal;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {

    @EntityGraph(attributePaths = "linkedAccounts")
    List<Goal> findByIsActiveTrueOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = "linkedAccounts")
    Optional<Goal> findById(UUID id);
}
