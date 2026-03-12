package com.retailmanagement.modules.report.dto.request;

import com.retailmanagement.modules.report.enums.ReportType;
import com.retailmanagement.modules.report.enums.ReportFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ReportRequest {

    private String reportName;

    private ReportType reportType;

    private ReportFormat format;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Map<String, String> parameters;

    private String description;
}