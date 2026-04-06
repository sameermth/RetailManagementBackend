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
@Entity(name = "ErpStoreProduct")
@Table(name = "store_product", indexes = {
        @Index(name = "idx_store_product_org_sku", columnList = "organization_id,sku", unique = true)
})
public class StoreProduct extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "base_uom_id", nullable = false)
    private Long baseUomId;

    @Column(name = "tax_group_id", nullable = false)
    private Long taxGroupId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "inventory_tracking_mode", nullable = false)
    private String inventoryTrackingMode;

    @Column(name = "serial_tracking_enabled", nullable = false)
    private Boolean serialTrackingEnabled = false;

    @Column(name = "batch_tracking_enabled", nullable = false)
    private Boolean batchTrackingEnabled = false;

    @Column(name = "expiry_tracking_enabled", nullable = false)
    private Boolean expiryTrackingEnabled = false;

    @Column(name = "fractional_quantity_allowed", nullable = false)
    private Boolean fractionalQuantityAllowed = false;

    @Column(name = "min_stock_base_qty", nullable = false)
    private BigDecimal minStockBaseQty = BigDecimal.ZERO;

    @Column(name = "reorder_level_base_qty", nullable = false)
    private BigDecimal reorderLevelBaseQty = BigDecimal.ZERO;

    @Column(name = "default_sale_price", precision = 18, scale = 2)
    private BigDecimal defaultSalePrice;

    @Column(name = "default_warranty_months")
    private Integer defaultWarrantyMonths;

    @Column(name = "warranty_terms")
    private String warrantyTerms;

    @Column(name = "is_service_item", nullable = false)
    private Boolean isServiceItem = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
