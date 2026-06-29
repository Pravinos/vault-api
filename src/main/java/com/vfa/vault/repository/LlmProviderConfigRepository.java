package com.vfa.vault.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vfa.vault.entity.LlmProviderConfig;

@Repository
public interface LlmProviderConfigRepository extends JpaRepository<LlmProviderConfig, Integer> {

    int CONFIG_ID = 1;

    default Optional<LlmProviderConfig> findConfig() {
        return findById(CONFIG_ID);
    }

    default LlmProviderConfig getConfig() {
        return findConfig()
                .orElseThrow(() -> new IllegalStateException("LLM config not found"));
    }
}
