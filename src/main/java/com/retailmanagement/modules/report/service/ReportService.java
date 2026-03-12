package com.retailmanagement.modules.report.service;

import com.retailmanagement.modules.report.dto.ReportRequest;
import com.retailmanagement.modules.report.dto.ReportResponse;

public interface ReportService {
    ReportResponse generateReport(ReportRequest request);
    String getReportType();
}

// New report types can be added without modifying existing code