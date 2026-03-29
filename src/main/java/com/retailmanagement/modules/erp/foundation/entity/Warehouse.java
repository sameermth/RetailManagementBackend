package com.retailmanagement.modules.erp.foundation.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity(name="ErpWarehouse") @Table(name="warehouse", indexes={@Index(name="idx_warehouse_org_branch_code", columnList="organization_id,branch_id,code", unique=true)})
public class Warehouse extends ErpOrgBranchScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String name;
 @Column(nullable=false) private String code;
 @Column(name="is_primary", nullable=false) private Boolean isPrimary = false;
 @Column(name="is_active", nullable=false) private Boolean isActive = true;
}
