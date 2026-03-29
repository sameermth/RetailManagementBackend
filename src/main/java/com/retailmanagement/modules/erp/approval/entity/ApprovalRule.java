package com.retailmanagement.modules.erp.approval.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_rule")
public class ApprovalRule extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "approval_type", nullable = false)
    private String approvalType;

    @Column(name = "min_amount", precision = 18, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 18, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "approver_role_id")
    private Long approverRoleId;

    @Column(name = "priority_order", nullable = false)
    private Integer priorityOrder = 1;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
