package com.retailmanagement.modules.erp.purchase.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_supplier_dispatch_notice_line", indexes = {
        @Index(name = "idx_po_supplier_dispatch_line_notice", columnList = "dispatch_notice_id"),
        @Index(name = "idx_po_supplier_dispatch_line_order_line", columnList = "purchase_order_line_id")
})
public class PurchaseOrderSupplierDispatchNoticeLine extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispatch_notice_id", nullable = false)
    private Long dispatchNoticeId;

    @Column(name = "purchase_order_line_id", nullable = false)
    private Long purchaseOrderLineId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "ordered_quantity_snapshot", nullable = false, precision = 18, scale = 6)
    private BigDecimal orderedQuantitySnapshot = BigDecimal.ZERO;

    @Column(name = "ordered_base_quantity_snapshot", nullable = false, precision = 18, scale = 6)
    private BigDecimal orderedBaseQuantitySnapshot = BigDecimal.ZERO;

    @Column(name = "dispatched_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal dispatchedQuantity = BigDecimal.ZERO;

    @Column(name = "dispatched_base_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal dispatchedBaseQuantity = BigDecimal.ZERO;

    @Column(name = "expected_remaining_dispatch_on")
    private LocalDate expectedRemainingDispatchOn;

    @Column(columnDefinition = "text")
    private String remarks;
}
