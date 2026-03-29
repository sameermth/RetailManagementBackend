package com.retailmanagement.modules.erp.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class ErpOrgScopedEntity extends ErpAuditableEntity {
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
}
