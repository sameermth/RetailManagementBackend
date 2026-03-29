package com.retailmanagement.modules.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardAnalyticsDTOs {
    private DashboardAnalyticsDTOs() {}

    public record ProfitabilityProductDTO(
            Long productId,
            String productName,
            String sku,
            BigDecimal revenue,
            BigDecimal cost,
            BigDecimal grossProfit,
            BigDecimal marginPercent
    ) {}

    public record ProfitabilitySummaryDTO(
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal revenue,
            BigDecimal cost,
            BigDecimal grossProfit,
            BigDecimal marginPercent,
            Integer invoiceCount,
            List<ProfitabilityProductDTO> topProducts
    ) {}

    public record AgingSummaryDTO(
            BigDecimal totalOutstanding,
            BigDecimal current,
            BigDecimal bucket1To30,
            BigDecimal bucket31To60,
            BigDecimal bucket61To90,
            BigDecimal bucket90Plus
    ) {}

    public record AgingDashboardDTO(
            LocalDate asOfDate,
            AgingSummaryDTO customers,
            AgingSummaryDTO suppliers
    ) {}

    public record StockProductSnapshotDTO(
            Long productId,
            String productName,
            String sku,
            BigDecimal onHandQuantity,
            BigDecimal reservedQuantity,
            BigDecimal availableQuantity,
            BigDecimal inventoryValue,
            String stockStatus
    ) {}

    public record StockSummaryDTO(
            BigDecimal onHandQuantity,
            BigDecimal reservedQuantity,
            BigDecimal availableQuantity,
            BigDecimal inventoryValue,
            Integer lowStockCount,
            Integer outOfStockCount,
            List<StockProductSnapshotDTO> lowStockProducts
    ) {}

    public record TaxSummaryDTO(
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal outputTax,
            BigDecimal inputTax,
            BigDecimal salesReturnTaxReversal,
            BigDecimal purchaseReturnTaxReversal,
            BigDecimal netTaxPayable,
            BigDecimal taxableSales,
            BigDecimal taxablePurchases,
            String gstAlertLevel,
            String gstMessage
    ) {}
}
