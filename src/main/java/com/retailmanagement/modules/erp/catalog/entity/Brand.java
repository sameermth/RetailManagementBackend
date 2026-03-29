package com.retailmanagement.modules.erp.catalog.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity(name="ErpBrand") @Table(name="brand")
public class Brand extends ErpOrgScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String name;
 @Column(name="is_active", nullable=false) private Boolean isActive = true;
}
