package com.retailmanagement.modules.report.dto.request;

import com.retailmanagement.modules.report.enums.ReportType;
import com.retailmanagement.modules.report.enums.ReportFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ScheduleReportRequest {

    private String scheduleName;

    private ReportType reportType;

    private ReportFormat format;

    private String frequency; // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM

    private String cronExpression;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Map<String, String> parameters;

    private String recipients;

    private String description;
}