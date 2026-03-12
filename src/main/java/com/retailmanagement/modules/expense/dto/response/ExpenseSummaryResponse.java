package com.retailmanagement.modules.expense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummaryResponse {
    private BigDecimal totalExpenses;
    private BigDecimal approvedExpenses;
    private BigDecimal pendingExpenses;
    private BigDecimal paidExpenses;
    private Map<String, BigDecimal> expensesByCategory;
    private Map<String, BigDecimal> expensesByMonth;
    private List<CategoryBreakdown> topCategories;
    private List<VendorBreakdown> topVendors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private String categoryName;
        private BigDecimal amount;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorBreakdown {
        private String vendor;
        private BigDecimal amount;
        private Integer count;
    }
}