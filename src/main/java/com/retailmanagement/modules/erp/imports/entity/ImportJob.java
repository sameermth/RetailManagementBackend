package com.retailmanagement.modules.erp.imports.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "import_job", indexes = {
        @Index(name = "idx_import_job_org_entity", columnList = "organization_id,entity_type,started_at"),
        @Index(name = "idx_import_job_org_status", columnList = "organization_id,status,started_at")
})
public class ImportJob extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "update_existing", nullable = false)
    private Boolean updateExisting = false;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows = 0;

    @Column(name = "valid_rows", nullable = false)
    private Integer validRows = 0;

    @Column(name = "imported_rows", nullable = false)
    private Integer importedRows = 0;

    @Column(name = "failed_rows", nullable = false)
    private Integer failedRows = 0;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
