package com.retailmanagement.modules.erp.sales.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sales_line_serial")
public class SalesLineSerial extends ErpAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_invoice_line_id", nullable = false)
    private Long salesInvoiceLineId;

    @Column(name = "serial_number_id", nullable = false)
    private Long serialNumberId;
}
