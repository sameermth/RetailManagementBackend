package com.retailmanagement.modules.erp.sales.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sales_quote_line")
public class SalesQuoteLine extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_quote_id", nullable = false)
    private Long salesQuoteId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "uom_id", nullable = false)
    private Long uomId;

    @Column(name = "hsn_snapshot")
    private String hsnSnapshot;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "base_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal baseQuantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "cgst_rate", precision = 10, scale = 4)
    private BigDecimal cgstRate;

    @Column(name = "cgst_amount", precision = 18, scale = 2)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_rate", precision = 10, scale = 4)
    private BigDecimal sgstRate;

    @Column(name = "sgst_amount", precision = 18, scale = 2)
    private BigDecimal sgstAmount;

    @Column(name = "igst_rate", precision = 10, scale = 4)
    private BigDecimal igstRate;

    @Column(name = "igst_amount", precision = 18, scale = 2)
    private BigDecimal igstAmount;

    @Column(name = "cess_rate", precision = 10, scale = 4)
    private BigDecimal cessRate;

    @Column(name = "cess_amount", precision = 18, scale = 2)
    private BigDecimal cessAmount;

    @Column(name = "line_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineAmount = BigDecimal.ZERO;

    @Column
    private String remarks;
}
