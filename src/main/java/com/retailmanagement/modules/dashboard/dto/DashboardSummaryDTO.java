package com.retailmanagement.modules.dashboard.dto;

import com.retailmanagement.modules.erp.tax.dto.TaxDtos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private SalesSummaryDTO todaySales;
    private SalesSummaryDTO weeklySales;
    private SalesSummaryDTO monthlySales;

    private Integer totalProducts;
    private Integer lowStockCount;
    private Integer outOfStockCount;

    private Integer totalCustomers;
    private Integer newCustomersToday;

    private BigDecimal totalDueAmount;
    private Integer overdueCount;

    private Integer pendingOrders;
    private Integer completedOrdersToday;
    private TaxDtos.GstThresholdStatusResponse gstStatus;
}
