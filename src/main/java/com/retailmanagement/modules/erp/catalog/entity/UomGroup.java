package com.retailmanagement.modules.erp.catalog.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="uom_group")
public class UomGroup extends ErpAuditableEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String name;
 @Column(nullable=false, unique=true) private String code;
 @Column(name="allows_fraction", nullable=false) private Boolean allowsFraction = false;
}
