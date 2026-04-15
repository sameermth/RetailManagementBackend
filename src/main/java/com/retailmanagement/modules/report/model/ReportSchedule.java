package com.retailmanagement.modules.report.model;

import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.report.enums.ReportFormat;
import com.retailmanagement.modules.report.enums.ReportType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "report_schedule")
@EntityListeners(AuditingEntityListener.class)
public class ReportSchedule {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User createdBy;

    @Column(name = "schedule_code", nullable = false)
    private String scheduleId;

    @Column(name = "report_type", nullable = false)
    private String reportTypeCode;

    @Column(nullable = false)
    private String frequency;

    @Column(name = "delivery_channel", nullable = false)
    private String deliveryChannel;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json")
    @Builder.Default
    private Map<String, Object> configJson = new HashMap<>();

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String scheduleName;

    @Transient
    private ReportType reportType;

    @Transient
    private ReportFormat format;

    @Transient
    private String cronExpression;

    @Transient
    private LocalDateTime startDate;

    @Transient
    private LocalDateTime endDate;

    @Transient
    private LocalDateTime lastRunDate;

    @Transient
    @Builder.Default
    private Map<String, String> parameters = new HashMap<>();

    @Transient
    private String recipients;

    @Transient
    @Builder.Default
    private Integer successCount = 0;

    @Transient
    @Builder.Default
    private Integer failureCount = 0;

    @Transient
    private String lastError;

    @PostLoad
    @PostPersist
    @PostUpdate
    void hydrateFromConfig() {
        Map<String, Object> config = configJson == null ? Map.of() : configJson;
        this.reportType = reportTypeFromCode(reportTypeCode);
        this.scheduleName = stringValue(config.get("scheduleName"));
        this.format = enumValue(config.get("format"), ReportFormat.class);
        if (this.format == null) {
            this.format = ReportFormat.PDF;
        }
        this.cronExpression = stringValue(config.get("cronExpression"));
        this.startDate = dateTimeValue(config.get("startDate"));
        this.endDate = dateTimeValue(config.get("endDate"));
        this.lastRunDate = dateTimeValue(config.get("lastRunDate"));
        this.parameters = mapValue(config.get("parameters"));
        this.recipients = stringValue(config.get("recipients"));
        this.successCount = integerValue(config.get("successCount"), 0);
        this.failureCount = integerValue(config.get("failureCount"), 0);
        this.lastError = stringValue(config.get("lastError"));
    }

    @PrePersist
    @PreUpdate
    void syncConfig() {
        Map<String, Object> config = configJson == null ? new HashMap<>() : new HashMap<>(configJson);
        this.reportTypeCode = reportType == null ? reportTypeCode : reportType.name();
        putOrRemove(config, "scheduleName", scheduleName);
        putOrRemove(config, "format", format == null ? null : format.name());
        putOrRemove(config, "cronExpression", cronExpression);
        putOrRemove(config, "startDate", startDate == null ? null : startDate.toString());
        putOrRemove(config, "endDate", endDate == null ? null : endDate.toString());
        putOrRemove(config, "lastRunDate", lastRunDate == null ? null : lastRunDate.toString());
        if (parameters == null || parameters.isEmpty()) {
            config.remove("parameters");
        } else {
            config.put("parameters", parameters);
        }
        putOrRemove(config, "recipients", recipients);
        config.put("successCount", successCount == null ? 0 : successCount);
        config.put("failureCount", failureCount == null ? 0 : failureCount);
        putOrRemove(config, "lastError", lastError);
        this.configJson = config;
    }

    private static void putOrRemove(Map<String, Object> config, String key, String value) {
        if (value == null || value.isBlank()) {
            config.remove(key);
        } else {
            config.put(key, value);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static LocalDateTime dateTimeValue(Object value) {
        if (value == null) {
            return null;
        }
        return LocalDateTime.parse(value.toString());
    }

    private static Integer integerValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static <E extends Enum<E>> E enumValue(Object value, Class<E> enumClass) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(enumClass, value.toString());
    }

    private static Map<String, String> mapValue(Object value) {
        if (value == null) {
            return new HashMap<>();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, String> result = new HashMap<>();
            map.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue == null ? null : String.valueOf(mapValue)));
            return result;
        }
        try {
            return OBJECT_MAPPER.readValue(value.toString(), new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return new HashMap<>();
        }
    }

    private static ReportType reportTypeFromCode(String reportTypeCode) {
        if (reportTypeCode == null || reportTypeCode.isBlank()) {
            return null;
        }
        return switch (reportTypeCode.trim().toUpperCase()) {
            case "DAILY_SALES_SUMMARY" -> ReportType.SALES_SUMMARY;
            case "DAILY_SALES_DETAILED" -> ReportType.SALES_DETAILED;
            case "INVENTORY_REPORT", "INVENTORY_SUMMARY_REPORT" -> ReportType.INVENTORY_SUMMARY;
            case "LOW_STOCK_ALERT", "LOW_STOCK_ALERT_REPORT", "LOW_STOCK_SUMMARY" -> ReportType.LOW_STOCK_REPORT;
            case "PURCHASE_REPORT", "PURCHASE_SUMMARY_REPORT" -> ReportType.PURCHASE_SUMMARY;
            case "CUSTOMER_DUES_REPORT" -> ReportType.CUSTOMER_DUES;
            case "GST_REPORT", "GST_SUMMARY", "TAX_SUMMARY" -> ReportType.TAX_REPORT;
            case "AUDIT_REPORT" -> ReportType.AUDIT_TRAIL;
            case "USER_ACTIVITY_REPORT" -> ReportType.USER_ACTIVITY;
            default -> {
                try {
                    yield ReportType.valueOf(reportTypeCode.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }
}
