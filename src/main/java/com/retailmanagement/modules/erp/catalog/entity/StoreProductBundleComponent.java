package com.retailmanagement.modules.erp.catalog.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "ErpStoreProductBundleComponent")
@Table(name = "store_product_bundle_component", indexes = {
        @Index(name = "idx_store_product_bundle_component_bundle", columnList = "organization_id,store_product_id,sort_order"),
        @Index(name = "idx_store_product_bundle_component_component", columnList = "organization_id,component_store_product_id")
})
public class StoreProductBundleComponent extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_product_id", nullable = false)
    private Long storeProductId;

    @Column(name = "component_store_product_id", nullable = false)
    private Long componentStoreProductId;

    @Column(name = "component_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal componentQuantity = BigDecimal.ONE;

    @Column(name = "component_base_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal componentBaseQuantity = BigDecimal.ONE;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
