package com.retailmanagement.modules.report.service;

import com.retailmanagement.modules.report.dto.request.ReportRequest;
import com.retailmanagement.modules.report.dto.response.ReportResponse;
import com.retailmanagement.modules.report.dto.response.ReportSummaryResponse;
import com.retailmanagement.modules.report.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportGeneratorService {

    ReportResponse generateReport(ReportRequest request, Long userId);

    ReportResponse getReportById(Long id);

    ReportResponse getReportByReportId(String reportId);

    Page<ReportResponse> getAllReports(Pageable pageable);

    List<ReportResponse> getReportsByType(ReportType reportType);

    Page<ReportResponse> getReportsByType(ReportType reportType, Pageable pageable);

    List<ReportResponse> getReportsByUser(Long userId);

    Page<ReportResponse> getReportsByUser(Long userId, Pageable pageable);

    List<ReportResponse> getReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    void deleteReport(Long id);

    byte[] downloadReport(Long id);

    void incrementDownloadCount(Long id);

    ReportSummaryResponse getDashboardSummary();

    // Specific report generation methods
    byte[] generateSalesReport(ReportRequest request);

    byte[] generateInventoryReport(ReportRequest request);

    byte[] generatePurchaseReport(ReportRequest request);

    byte[] generateFinancialReport(ReportRequest request);

    byte[] generateCustomerReport(ReportRequest request);

    byte[] generateExpenseReport(ReportRequest request);

    byte[] generateTaxReport(ReportRequest request);
}