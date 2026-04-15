package com.retailmanagement.modules.erp.imports.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "import_job_row", indexes = {
        @Index(name = "idx_import_job_row_job", columnList = "import_job_id,row_number"),
        @Index(name = "idx_import_job_row_job_status", columnList = "import_job_id,status")
})
public class ImportJobRow extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_job_id", nullable = false)
    private Long importJobId;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 255)
    private String reference;

    @Column(name = "messages_json", columnDefinition = "text")
    private String messagesJson;

    @Column(name = "values_json", columnDefinition = "text")
    private String valuesJson;
}
