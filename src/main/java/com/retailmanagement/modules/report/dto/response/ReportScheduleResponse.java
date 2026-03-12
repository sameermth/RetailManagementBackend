package com.retailmanagement.modules.report.dto.response;

import com.retailmanagement.modules.report.enums.ReportType;
import com.retailmanagement.modules.report.enums.ReportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportScheduleResponse {
    private Long id;
    private String scheduleId;
    private String scheduleName;
    private ReportType reportType;
    private ReportFormat format;
    private String createdBy;
    private String frequency;
    private String cronExpression;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime lastRunDate;
    private LocalDateTime nextRunDate;
    private Map<String, String> parameters;
    private String recipients;
    private Boolean isActive;
    private Integer successCount;
    private Integer failureCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}