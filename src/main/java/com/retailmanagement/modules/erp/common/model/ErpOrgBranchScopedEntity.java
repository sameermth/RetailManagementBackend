package com.retailmanagement.modules.erp.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class ErpOrgBranchScopedEntity extends ErpOrgScopedEntity {
    @Column(name = "branch_id", nullable = false)
    private Long branchId;
}
