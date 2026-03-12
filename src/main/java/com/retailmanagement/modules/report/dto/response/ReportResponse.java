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
public class ReportResponse {
    private Long id;
    private String reportId;
    private String reportName;
    private ReportType reportType;
    private ReportFormat format;
    private String generatedBy;
    private LocalDateTime generatedDate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String description;
    private String fileUrl;
    private Long fileSize;
    private Map<String, String> parameters;
    private String status;
    private Integer downloadCount;
    private LocalDateTime createdAt;
}