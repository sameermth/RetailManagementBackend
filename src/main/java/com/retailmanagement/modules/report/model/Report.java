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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reports")
@EntityListeners(AuditingEntityListener.class)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reportId;

    @Column(nullable = false)
    private String reportName;

    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    private ReportFormat format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    private LocalDateTime generatedDate;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Column(length = 1000)
    private String description;

    private String filePath;

    private String fileUrl;

    private Long fileSize;

    @ElementCollection
    @CollectionTable(name = "report_parameters",
            joinColumns = @JoinColumn(name = "report_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value", length = 500)
    @Builder.Default
    private Map<String, String> parameters = new HashMap<>();

    @Builder.Default
    private Boolean isScheduled = false;

    private String scheduleCron;

    private LocalDateTime nextScheduledDate;

    private String recipients; // Comma separated emails

    @Builder.Default
    private Integer downloadCount = 0;

    private String status; // GENERATING, COMPLETED, FAILED

    private String errorMessage;

    @CreatedDate
    private LocalDateTime createdAt;
}
