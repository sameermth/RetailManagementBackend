package com.retailmanagement.modules.erp.catalog.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="uom")
public class Uom extends ErpAuditableEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="uom_group_id", nullable=false) private Long uomGroupId;
 @Column(nullable=false) private String name;
 @Column(nullable=false, unique=true) private String code;
 @Column(name="decimal_scale", nullable=false) private Integer decimalScale = 0;
 @Column(name="is_active", nullable=false) private Boolean isActive = true;
}
