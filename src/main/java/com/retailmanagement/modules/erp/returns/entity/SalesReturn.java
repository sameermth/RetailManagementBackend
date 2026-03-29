package com.retailmanagement.modules.erp.returns.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sales_return")
public class SalesReturn extends ErpOrgBranchScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    @Column(name = "original_sales_invoice_id", nullable = false)
    private Long originalSalesInvoiceId;
    @Column(name = "return_number", nullable = false)
    private String returnNumber;
    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;
    @Column(name = "seller_gstin")
    private String sellerGstin;
    @Column(name = "customer_gstin")
    private String customerGstin;
    @Column(name = "place_of_supply_state_code")
    private String placeOfSupplyStateCode;
    private String reason;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String remarks;
    @Column(name = "posted_at")
    private LocalDateTime postedAt;
    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;
    @Column(name = "inspected_by")
    private Long inspectedBy;
    @Column(name = "inspection_notes")
    private String inspectionNotes;
}
