package com.retailmanagement.modules.erp.returns.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "purchase_return_line_serial")
public class PurchaseReturnLineSerial extends ErpAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_return_line_id", nullable = false)
    private Long purchaseReturnLineId;

    @Column(name = "serial_number_id", nullable = false)
    private Long serialNumberId;
}
