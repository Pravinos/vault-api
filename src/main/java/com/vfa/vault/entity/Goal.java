package com.vfa.vault.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @UuidGenerator
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "target_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "saved_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal savedAmount = BigDecimal.ZERO;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "goal_accounts",
        joinColumns = @JoinColumn(name = "goal_id"),
        inverseJoinColumns = @JoinColumn(name = "account_id")
    )
    private Set<Account> linkedAccounts = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 20)
    private GoalType goalType;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (savedAmount == null) savedAmount = BigDecimal.ZERO;
    }
}
