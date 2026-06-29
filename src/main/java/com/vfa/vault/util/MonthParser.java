package com.vfa.vault.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class MonthParser {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String INVALID_MONTH_MESSAGE = "Month must be in YYYY-MM format";

    private MonthParser() {
    }

    public static LocalDate parseMonthStart(String month) {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException(INVALID_MONTH_MESSAGE);
        }
        try {
            return YearMonth.parse(month.trim(), MONTH_FMT).atDay(1);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(INVALID_MONTH_MESSAGE);
        }
    }

    public static YearMonth parseYearMonth(String month) {
        return YearMonth.from(parseMonthStart(month));
    }

    public static String formatMonth(LocalDate monthStart) {
        return YearMonth.from(monthStart).format(MONTH_FMT);
    }

    public static String currentMonth() {
        return YearMonth.now().format(MONTH_FMT);
    }
}
