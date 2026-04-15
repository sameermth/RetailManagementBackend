package com.retailmanagement.modules.erp.purchase.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "purchase_receipt_line_batch")
public class PurchaseReceiptLineBatch extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_receipt_line_id", nullable = false)
    private Long purchaseReceiptLineId;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "base_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal baseQuantity;

    @Column(name = "suggested_sale_price", precision = 18, scale = 2)
    private BigDecimal suggestedSalePrice;

    @Column(name = "mrp", precision = 18, scale = 2)
    private BigDecimal mrp;
}
