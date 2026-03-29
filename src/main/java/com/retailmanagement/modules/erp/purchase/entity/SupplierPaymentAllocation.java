package com.retailmanagement.modules.erp.purchase.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "supplier_payment_allocation")
public class SupplierPaymentAllocation extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_payment_id", nullable = false)
    private Long supplierPaymentId;

    @Column(name = "purchase_receipt_id", nullable = false)
    private Long purchaseReceiptId;

    @Column(name = "allocated_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal allocatedAmount;
}
