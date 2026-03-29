package com.retailmanagement.modules.erp.approval.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_request", indexes = {
        @Index(name = "idx_erp_approval_request_entity", columnList = "entity_type,entity_id"),
        @Index(name = "idx_erp_approval_request_status", columnList = "status,requested_at")
})
public class ApprovalRequest extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "entity_number")
    private String entityNumber;

    @Column(name = "approval_type", nullable = false)
    private String approvalType;

    @Column(nullable = false)
    private String status;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "current_approver_user_id")
    private Long currentApproverUserId;

    @Column(name = "current_approver_role_snapshot")
    private String currentApproverRoleSnapshot;

    @Column(name = "request_reason", columnDefinition = "text")
    private String requestReason;
}
