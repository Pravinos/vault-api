package com.vfa.vault.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "investment_details")
public class InvestmentDetail {

    @Id
    @UuidGenerator
    @EqualsAndHashCode.Include
    private UUID id;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(length = 100)
    private String platform;

    @Column(length = 100)
    private String instrument;

    @Column(name = "asset_type", length = 50)
    private String assetType;
}
