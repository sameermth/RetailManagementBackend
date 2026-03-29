package com.retailmanagement.modules.erp.party.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "ErpSupplierProduct")
@Table(name = "supplier_product", indexes = {
        @Index(name = "idx_erp_supplier_product_supplier", columnList = "organization_id,supplier_id,is_active"),
        @Index(name = "idx_erp_supplier_product_product", columnList = "organization_id,product_id,is_active")
})
public class SupplierProduct extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "supplier_product_code")
    private String supplierProductCode;

    @Column(name = "supplier_product_name")
    private String supplierProductName;

    @Column(nullable = false)
    private Integer priority = 1;

    @Column(name = "is_preferred", nullable = false)
    private Boolean isPreferred = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
