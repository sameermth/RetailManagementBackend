package com.retailmanagement.modules.dashboard.service;

import com.retailmanagement.modules.dashboard.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    DashboardSummaryDTO getDashboardSummary();
    SalesSummaryDTO getTodaySales();
    SalesSummaryDTO getSalesForPeriod(LocalDate startDate, LocalDate endDate);
    List<TopProductDTO> getTopProducts(int limit);
    List<LowStockAlertDTO> getLowStockAlerts();
    List<RecentActivityDTO> getRecentActivities(int limit);
    DueSummaryDTO getDueSummary();
    List<DueSummaryDTO.UpcomingDueDTO> getUpcomingDues(int days);
}