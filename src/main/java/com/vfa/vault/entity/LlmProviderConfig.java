package com.vfa.vault.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "llm_provider_config")
public class LlmProviderConfig {

    @Id
    private Integer id;

    @Column(name = "chat_provider", nullable = false, length = 20)
    private String chatProvider;

    @Column(name = "chat_model", nullable = false, length = 100)
    private String chatModel;

    @Column(name = "summary_provider", nullable = false, length = 20)
    private String summaryProvider;

    @Column(name = "summary_model", nullable = false, length = 100)
    private String summaryModel;

    @Column(name = "lmstudio_models", columnDefinition = "TEXT")
    private String lmstudioModels;

    @Column(name = "groq_models", columnDefinition = "TEXT")
    private String groqModels;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
