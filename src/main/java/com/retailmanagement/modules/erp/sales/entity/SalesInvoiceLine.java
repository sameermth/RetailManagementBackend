package com.retailmanagement.modules.erp.sales.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="sales_invoice_line")
public class SalesInvoiceLine extends ErpAuditableEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="sales_invoice_id", nullable=false) private Long salesInvoiceId;
 @Column(name="product_id", nullable=false) private Long productId;
 @Column(name="uom_id", nullable=false) private Long uomId;
 @Column(name="hsn_snapshot") private String hsnSnapshot;
 @Column(nullable=false, precision=18, scale=6) private BigDecimal quantity;
 @Column(name="base_quantity", nullable=false, precision=18, scale=6) private BigDecimal baseQuantity;
 @Column(name="unit_price", nullable=false, precision=18, scale=2) private BigDecimal unitPrice;
 @Column(name="mrp", precision=18, scale=2) private BigDecimal mrp;
 @Column(name="discount_amount", nullable=false, precision=18, scale=2) private BigDecimal discountAmount;
 @Column(name="tax_rate", nullable=false, precision=18, scale=2) private BigDecimal taxRate;
 @Column(name="taxable_amount", nullable=false, precision=18, scale=2) private BigDecimal taxableAmount = BigDecimal.ZERO;
 @Column(name="cgst_rate", nullable=false, precision=9, scale=4) private BigDecimal cgstRate = BigDecimal.ZERO;
 @Column(name="cgst_amount", nullable=false, precision=18, scale=2) private BigDecimal cgstAmount = BigDecimal.ZERO;
 @Column(name="sgst_rate", nullable=false, precision=9, scale=4) private BigDecimal sgstRate = BigDecimal.ZERO;
 @Column(name="sgst_amount", nullable=false, precision=18, scale=2) private BigDecimal sgstAmount = BigDecimal.ZERO;
 @Column(name="igst_rate", nullable=false, precision=9, scale=4) private BigDecimal igstRate = BigDecimal.ZERO;
 @Column(name="igst_amount", nullable=false, precision=18, scale=2) private BigDecimal igstAmount = BigDecimal.ZERO;
 @Column(name="cess_rate", nullable=false, precision=9, scale=4) private BigDecimal cessRate = BigDecimal.ZERO;
 @Column(name="cess_amount", nullable=false, precision=18, scale=2) private BigDecimal cessAmount = BigDecimal.ZERO;
 @Column(name="line_amount", nullable=false, precision=18, scale=2) private BigDecimal lineAmount;
 @Column(name="warranty_months") private Integer warrantyMonths;
 @Column(name="unit_cost_at_sale", nullable=false, precision=18, scale=2) private BigDecimal unitCostAtSale = BigDecimal.ZERO;
 @Column(name="total_cost_at_sale", nullable=false, precision=18, scale=2) private BigDecimal totalCostAtSale = BigDecimal.ZERO;
}
