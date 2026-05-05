package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDashboardDTO {

    private UUID id;
    private String name;
    private String accountType;

    private BigDecimal calculatedBalance;
    private BigDecimal manualBalance;
    private BigDecimal openingBalance;

    private BigDecimal sinceOpening;

    private BigDecimal currentValue;
    private BigDecimal returnAmount;
    private BigDecimal returnPercentage;

    private String secondaryLabel;
    private boolean secondaryPositive;
}
