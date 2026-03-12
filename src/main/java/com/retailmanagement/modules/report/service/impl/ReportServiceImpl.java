package com.retailmanagement.modules.report.service.impl;

import com.retailmanagement.modules.report.dto.ReportRequest;
import com.retailmanagement.modules.report.dto.ReportResponse;
import com.retailmanagement.modules.report.service.ReportService;
import com.retailmanagement.modules.sales.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalesReportServiceImpl implements ReportService {

    private final SalesService salesService;

    @Override
    public ReportResponse generateReport(ReportRequest request) {
        // Sales-specific report generation
        Double totalSales = salesService.getTotalSales(
                request.getStartDate(),
                request.getEndDate()
        );

        return ReportResponse.builder()
                .type("SALES_REPORT")
                .data(Map.of("totalSales", totalSales))
                .build();
    }

    @Override
    public String getReportType() {
        return "SALES";
    }
}

// Add new report type without modifying existing code
@Service
public class InventoryReportServiceImpl implements ReportService {
    @Override
    public ReportResponse generateReport(ReportRequest request) {
        // Inventory-specific logic
    }

    @Override
    public String getReportType() {
        return "INVENTORY";
    }
}