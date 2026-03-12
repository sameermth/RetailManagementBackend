package com.retailmanagement.modules.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {

    // Sales Summary
    private SalesSummary salesSummary;

    // Inventory Summary
    private InventorySummary inventorySummary;

    // Financial Summary
    private FinancialSummary financialSummary;

    // Customer Summary
    private CustomerSummary customerSummary;

    // Charts Data
    private Map<String, Object> charts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesSummary {
        private Double totalSales;
        private Long totalOrders;
        private Double averageOrderValue;
        private List<Map<String, Object>> salesByDay;
        private List<Map<String, Object>> topProducts;
        private List<Map<String, Object>> salesByPaymentMethod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventorySummary {
        private Long totalProducts;
        private Long lowStockItems;
        private Long outOfStockItems;
        private Double totalInventoryValue;
        private List<Map<String, Object>> stockByCategory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialSummary {
        private Double revenue;
        private Double expenses;
        private Double profit;
        private Double profitMargin;
        private List<Map<String, Object>> revenueByMonth;
        private List<Map<String, Object>> expensesByCategory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSummary {
        private Long totalCustomers;
        private Long newCustomers;
        private Double totalDues;
        private Long overdueCount;
        private List<Map<String, Object>> topCustomers;
    }
}