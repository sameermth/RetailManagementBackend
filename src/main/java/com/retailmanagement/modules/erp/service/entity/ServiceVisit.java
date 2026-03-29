package com.retailmanagement.modules.erp.service.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "service_visit", indexes = {
        @Index(name = "idx_service_visit_ticket", columnList = "service_ticket_id"),
        @Index(name = "idx_service_visit_technician", columnList = "technician_user_id")
})
public class ServiceVisit extends ErpOrgBranchScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_ticket_id", nullable = false)
    private Long serviceTicketId;

    @Column(name = "technician_user_id")
    private Long technicianUserId;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "visit_status", nullable = false)
    private String visitStatus;

    @Column(name = "visit_notes")
    private String visitNotes;

    @Column(name = "parts_used_json", columnDefinition = "jsonb")
    private String partsUsedJson;

    @Column(name = "customer_feedback")
    private String customerFeedback;
}
