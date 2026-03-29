package com.retailmanagement.modules.erp.approval.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_history")
public class ApprovalHistory extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "approval_request_id", nullable = false)
    private Long approvalRequestId;

    @Column(name = "approver_user_id")
    private Long approverUserId;

    @Column(nullable = false)
    private String action;

    @Column(name = "approver_role_snapshot")
    private String approverRoleSnapshot;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;
}
