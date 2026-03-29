package com.retailmanagement.modules.erp.catalog.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
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
@Entity(name = "ErpMasterProduct")
@Table(name = "product", indexes = {
        @Index(name = "idx_product_name_brand", columnList = "name,brand_name"),
        @Index(name = "idx_product_hsn_code", columnList = "hsn_code")
})
public class Product extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "base_uom_id", nullable = false)
    private Long baseUomId;

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

    @Column(name = "is_service_item", nullable = false)
    private Boolean isServiceItem = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
