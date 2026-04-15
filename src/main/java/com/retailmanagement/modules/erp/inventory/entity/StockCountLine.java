package com.retailmanagement.modules.erp.inventory.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
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
@Entity
@Table(name = "stock_count_line", indexes = {
        @Index(name = "idx_stock_count_line_session", columnList = "stock_count_session_id")
})
public class StockCountLine extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_count_session_id", nullable = false)
    private Long stockCountSessionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "expected_base_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal expectedBaseQuantity;

    @Column(name = "counted_base_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal countedBaseQuantity;

    @Column(name = "variance_base_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal varianceBaseQuantity;

    @Column(name = "posted_adjustment_id")
    private Long postedAdjustmentId;

    @Column
    private String remarks;
}
