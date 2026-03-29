package com.retailmanagement.modules.erp.purchase.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "purchase_receipt_line_serial")
public class PurchaseReceiptLineSerial extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_receipt_line_id", nullable = false)
    private Long purchaseReceiptLineId;

    @Column(name = "serial_number_id", nullable = false)
    private Long serialNumberId;
}
