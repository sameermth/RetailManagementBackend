package com.retailmanagement.modules.erp.common.util;

import com.retailmanagement.common.exceptions.BusinessException;
import java.time.LocalDate;

public final class RecurringScheduleSupport {
    private RecurringScheduleSupport() {}

    public static String normalizeFrequency(String frequency) {
        if (frequency == null || frequency.isBlank()) {
            throw new BusinessException("Frequency is required");
        }
        String normalized = frequency.trim().toUpperCase();
        if (!normalized.equals("DAILY")
                && !normalized.equals("WEEKLY")
                && !normalized.equals("MONTHLY")
                && !normalized.equals("QUARTERLY")
                && !normalized.equals("YEARLY")) {
            throw new BusinessException("Unsupported recurrence frequency: " + frequency);
        }
        return normalized;
    }

    public static LocalDate nextRunDate(String frequency, LocalDate runDate) {
        String normalized = normalizeFrequency(frequency);
        return switch (normalized) {
            case "DAILY" -> runDate.plusDays(1);
            case "WEEKLY" -> runDate.plusWeeks(1);
            case "MONTHLY" -> runDate.plusMonths(1);
            case "QUARTERLY" -> runDate.plusMonths(3);
            case "YEARLY" -> runDate.plusYears(1);
            default -> throw new BusinessException("Unsupported recurrence frequency: " + frequency);
        };
    }
}
