package com.retailmanagement.modules.platformadmin.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
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
@Table(name = "platform_incident", indexes = {
        @Index(name = "idx_platform_incident_status", columnList = "status,opened_at"),
        @Index(name = "idx_platform_incident_product", columnList = "product_id"),
        @Index(name = "idx_platform_incident_org", columnList = "organization_id")
})
public class PlatformIncident extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_number", nullable = false, unique = true)
    private String incidentNumber;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "subject_type", nullable = false)
    private String subjectType;

    @Column(name = "incident_type", nullable = false)
    private String incidentType;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "store_product_id")
    private Long storeProductId;

    @Column(name = "service_ticket_id")
    private Long serviceTicketId;

    @Column(name = "warranty_claim_id")
    private Long warrantyClaimId;

    @Column(name = "reported_by")
    private String reportedBy;

    @Column(name = "recommended_action")
    private String recommendedAction;

    @Column(name = "action_taken")
    private String actionTaken;

    @Column(name = "resolution_notes")
    private String resolutionNotes;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
