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
@Table(name = "purchase_return")
public class PurchaseReturn extends ErpOrgBranchScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;
    @Column(name = "original_purchase_receipt_id", nullable = false)
    private Long originalPurchaseReceiptId;
    @Column(name = "return_number", nullable = false)
    private String returnNumber;
    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;
    @Column(name = "seller_gstin")
    private String sellerGstin;
    @Column(name = "supplier_gstin")
    private String supplierGstin;
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
}
