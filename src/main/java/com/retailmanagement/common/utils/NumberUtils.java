package com.retailmanagement.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberUtils {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat QUANTITY_FORMAT = new DecimalFormat("#,##0");

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return CURRENCY_FORMAT.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    public static String formatCurrency(Double amount) {
        if (amount == null) return "0.00";
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatPercentage(BigDecimal percentage) {
        if (percentage == null) return "0.00%";
        return PERCENTAGE_FORMAT.format(percentage.setScale(2, RoundingMode.HALF_UP)) + "%";
    }

    public static String formatPercentage(Double percentage) {
        if (percentage == null) return "0.00%";
        return PERCENTAGE_FORMAT.format(percentage) + "%";
    }

    public static String formatQuantity(Integer quantity) {
        if (quantity == null) return "0";
        return QUANTITY_FORMAT.format(quantity);
    }

    public static String formatQuantity(Long quantity) {
        if (quantity == null) return "0";
        return QUANTITY_FORMAT.format(quantity);
    }

    public static BigDecimal calculatePercentage(BigDecimal value, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(new BigDecimal(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    public static double calculatePercentage(double value, double total) {
        if (total == 0) return 0;
        return (value / total) * 100;
    }

    public static BigDecimal round(BigDecimal value, int places) {
        if (value == null) return BigDecimal.ZERO;
        return value.setScale(places, RoundingMode.HALF_UP);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    public static boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    public static String toWords(Integer number) {
        // Implementation for number to words conversion
        // This would be a comprehensive method for check printing etc.
        return String.valueOf(number); // Placeholder
    }
}