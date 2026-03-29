package com.retailmanagement.modules.erp.inventory.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stock_adjustment_line")
public class StockAdjustmentLine extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_adjustment_id", nullable = false)
    private Long stockAdjustmentId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "uom_id", nullable = false)
    private Long uomId;

    @Column(name = "quantity_delta", nullable = false, precision = 18, scale = 6)
    private BigDecimal quantityDelta;

    @Column(name = "base_quantity_delta", nullable = false, precision = 18, scale = 6)
    private BigDecimal baseQuantityDelta;

    @Column(name = "unit_cost", precision = 18, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "line_reason")
    private String lineReason;
}
