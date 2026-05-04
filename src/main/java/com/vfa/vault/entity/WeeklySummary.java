package com.vfa.vault.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "weekly_summaries")
public class WeeklySummary {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "total_spent", precision = 10, scale = 2)
    private BigDecimal totalSpent;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "provider", length = 20)
    private String provider;

    @Column(name = "model", length = 100)
    private String model;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}