package com.retailmanagement.modules.erp.sales.entity;

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
@Table(name = "sales_dispatch_line", indexes = {
        @Index(name = "idx_sales_dispatch_line_dispatch", columnList = "sales_dispatch_id"),
        @Index(name = "idx_sales_dispatch_line_invoice_line", columnList = "sales_invoice_line_id")
})
public class SalesDispatchLine extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_dispatch_id", nullable = false)
    private Long salesDispatchId;

    @Column(name = "sales_invoice_line_id", nullable = false)
    private Long salesInvoiceLineId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "uom_id", nullable = false)
    private Long uomId;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "base_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal baseQuantity;

    @Column(name = "picked_quantity", precision = 18, scale = 6)
    private BigDecimal pickedQuantity;

    @Column(name = "picked_base_quantity", precision = 18, scale = 6)
    private BigDecimal pickedBaseQuantity;

    @Column(name = "picked_bin_location_id")
    private Long pickedBinLocationId;

    @Column(name = "packed_quantity", precision = 18, scale = 6)
    private BigDecimal packedQuantity;

    @Column(name = "packed_base_quantity", precision = 18, scale = 6)
    private BigDecimal packedBaseQuantity;

    @Column
    private String remarks;
}
