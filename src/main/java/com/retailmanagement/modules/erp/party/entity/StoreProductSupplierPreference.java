package com.retailmanagement.modules.erp.party.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "ErpStoreProductSupplierPreference")
@Table(name = "store_product_supplier_preference", indexes = {
        @Index(name = "idx_erp_store_product_supplier_pref_product", columnList = "organization_id,store_product_id,is_active"),
        @Index(name = "idx_erp_store_product_supplier_pref_supplier", columnList = "organization_id,supplier_id,is_active")
})
public class StoreProductSupplierPreference extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_product_id", nullable = false)
    private Long storeProductId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "supplier_product_id", nullable = false)
    private Long supplierProductId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "remarks")
    private String remarks;
}
