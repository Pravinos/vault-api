package com.vfa.vault.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseHeatmapDTO(
        int year,
        List<DayTotal> days,
        BigDecimal maxDayAmount
) {
    public record DayTotal(String date, BigDecimal totalAmount) {}
}
