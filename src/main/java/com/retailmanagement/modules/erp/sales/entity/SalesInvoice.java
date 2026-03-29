package com.retailmanagement.modules.erp.sales.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="sales_invoice")
public class SalesInvoice extends ErpOrgBranchScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="warehouse_id", nullable=false) private Long warehouseId;
 @Column(name="customer_id", nullable=false) private Long customerId;
 @Column(name="price_list_id") private Long priceListId;
 @Column(name="invoice_number", nullable=false) private String invoiceNumber;
 @Column(name="invoice_date", nullable=false) private LocalDate invoiceDate;
 @Column(name="due_date", nullable=false) private LocalDate dueDate;
 @Column(name="seller_tax_registration_id") private Long sellerTaxRegistrationId;
 @Column(name="seller_gstin") private String sellerGstin;
 @Column(name="customer_gstin") private String customerGstin;
 @Column(name="place_of_supply_state_code") private String placeOfSupplyStateCode;
 @Column(nullable=false) private String status;
 @Column(nullable=false, precision=18, scale=2) private BigDecimal subtotal;
 @Column(name="discount_amount", nullable=false, precision=18, scale=2) private BigDecimal discountAmount;
 @Column(name="tax_amount", nullable=false, precision=18, scale=2) private BigDecimal taxAmount;
 @Column(name="total_amount", nullable=false, precision=18, scale=2) private BigDecimal totalAmount;
 @Column private String remarks;
 @Column(name="posted_at") private java.time.LocalDateTime postedAt;
 @Column(name="cancelled_at") private java.time.LocalDateTime cancelledAt;
 @Column(name="cancelled_by") private Long cancelledBy;
 @Column(name="cancel_reason") private String cancelReason;
}
