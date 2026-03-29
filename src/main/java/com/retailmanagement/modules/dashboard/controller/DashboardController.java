package com.retailmanagement.modules.dashboard.controller;

import com.retailmanagement.modules.dashboard.dto.DashboardSummaryDTO;
import com.retailmanagement.modules.dashboard.dto.DashboardAnalyticsDTOs;
import com.retailmanagement.modules.dashboard.dto.SalesSummaryDTO;
import com.retailmanagement.modules.dashboard.dto.TopProductDTO;
import com.retailmanagement.modules.dashboard.dto.LowStockAlertDTO;
import com.retailmanagement.modules.dashboard.dto.RecentActivityDTO;
import com.retailmanagement.modules.dashboard.dto.DueSummaryDTO;
import com.retailmanagement.modules.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard data endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }

    @GetMapping("/sales/today")
    @Operation(summary = "Get today's sales summary")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<SalesSummaryDTO> getTodaySales() {
        return ResponseEntity.ok(dashboardService.getTodaySales());
    }

    @GetMapping("/sales/period")
    @Operation(summary = "Get sales summary for date range")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<SalesSummaryDTO> getSalesForPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(dashboardService.getSalesForPeriod(startDate, endDate));
    }

    @GetMapping("/products/top")
    @Operation(summary = "Get top selling products")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<List<TopProductDTO>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getTopProducts(limit));
    }

    @GetMapping("/inventory/low-stock")
    @Operation(summary = "Get low stock alerts")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<List<LowStockAlertDTO>> getLowStockAlerts() {
        return ResponseEntity.ok(dashboardService.getLowStockAlerts());
    }

    @GetMapping("/activities/recent")
    @Operation(summary = "Get recent activities")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<List<RecentActivityDTO>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentActivities(limit));
    }

    @GetMapping("/dues/summary")
    @Operation(summary = "Get due payments summary")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<DueSummaryDTO> getDueSummary() {
        return ResponseEntity.ok(dashboardService.getDueSummary());
    }

    @GetMapping("/dues/upcoming")
    @Operation(summary = "Get upcoming dues")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<List<DueSummaryDTO.UpcomingDueDTO>> getUpcomingDues(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(dashboardService.getUpcomingDues(days));
    }

    @GetMapping("/profitability")
    @Operation(summary = "Get profitability summary")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<DashboardAnalyticsDTOs.ProfitabilitySummaryDTO> getProfitabilitySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getProfitabilitySummary(startDate, endDate, limit));
    }

    @GetMapping("/aging")
    @Operation(summary = "Get customer and supplier aging summary")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<DashboardAnalyticsDTOs.AgingDashboardDTO> getAgingDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(dashboardService.getAgingDashboard(asOfDate));
    }

    @GetMapping("/inventory/stock-summary")
    @Operation(summary = "Get stock snapshot summary")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<DashboardAnalyticsDTOs.StockSummaryDTO> getStockSummary(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getStockSummary(limit));
    }

    @GetMapping("/tax/summary")
    @Operation(summary = "Get tax summary")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ResponseEntity<DashboardAnalyticsDTOs.TaxSummaryDTO> getTaxSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(dashboardService.getTaxSummary(startDate, endDate));
    }
}
