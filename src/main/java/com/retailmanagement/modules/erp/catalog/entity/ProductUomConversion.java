package com.retailmanagement.modules.erp.catalog.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="product_uom_conversion")
public class ProductUomConversion extends ErpAuditableEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="product_id", nullable=false) private Long productId;
 @Column(name="from_uom_id", nullable=false) private Long fromUomId;
 @Column(name="to_uom_id", nullable=false) private Long toUomId;
 @Column(nullable=false, precision=18, scale=6) private BigDecimal multiplier;
 @Column(name="is_purchase_uom", nullable=false) private Boolean isPurchaseUom = false;
 @Column(name="is_sales_uom", nullable=false) private Boolean isSalesUom = false;
 @Column(name="is_default", nullable=false) private Boolean isDefault = false;
}
