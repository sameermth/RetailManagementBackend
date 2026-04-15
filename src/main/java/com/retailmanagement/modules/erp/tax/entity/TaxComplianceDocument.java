package com.retailmanagement.modules.erp.tax.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tax_compliance_document", indexes = {
        @Index(name = "idx_tax_compliance_source", columnList = "organization_id,source_type,source_id"),
        @Index(name = "idx_tax_compliance_type", columnList = "organization_id,document_type,status")
})
public class TaxComplianceDocument extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;

    @Column(name = "provider_code", nullable = false, length = 40)
    private String providerCode;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "eligible_for_submission", nullable = false)
    private Boolean eligibleForSubmission = Boolean.FALSE;

    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    @Column(name = "response_json", columnDefinition = "text")
    private String responseJson;

    @Column(name = "warning_json", columnDefinition = "text")
    private String warningJson;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(name = "acknowledgement_number", length = 120)
    private String acknowledgementNumber;

    @Column(name = "acknowledgement_date_time")
    private LocalDateTime acknowledgementDateTime;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
