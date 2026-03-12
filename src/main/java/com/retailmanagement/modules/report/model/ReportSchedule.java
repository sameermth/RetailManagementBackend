package com.retailmanagement.modules.report.model;

import com.retailmanagement.modules.report.enums.ReportType;
import com.retailmanagement.modules.report.enums.ReportFormat;
import com.retailmanagement.modules.auth.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "report_schedules")
@EntityListeners(AuditingEntityListener.class)
public class ReportSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String scheduleId;

    private String scheduleName;

    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    private ReportFormat format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private String frequency; // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM

    private String cronExpression;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LocalDateTime lastRunDate;

    private LocalDateTime nextRunDate;

    @ElementCollection
    @CollectionTable(name = "schedule_parameters",
            joinColumns = @JoinColumn(name = "schedule_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value", length = 500)
    private Map<String, String> parameters = new HashMap<>();

    private String recipients; // Comma separated emails

    private Boolean isActive = true;

    private Integer successCount = 0;

    private Integer failureCount = 0;

    private String lastError;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}