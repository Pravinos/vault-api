package com.vfa.vault.repository;

import com.vfa.vault.entity.LlmProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LlmProviderConfigRepository extends JpaRepository<LlmProviderConfig, Integer> {
}
