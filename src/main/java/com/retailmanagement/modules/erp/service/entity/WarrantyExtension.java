package com.retailmanagement.modules.erp.service.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "warranty_extension", indexes = {
        @Index(name = "idx_warranty_extension_ownership", columnList = "organization_id,product_ownership_id,status,id")
})
public class WarrantyExtension extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_ownership_id", nullable = false)
    private Long productOwnershipId;

    @Column(name = "serial_number_id")
    private Long serialNumberId;

    @Column(name = "sales_invoice_id")
    private Long salesInvoiceId;

    @Column(name = "sales_invoice_line_id")
    private Long salesInvoiceLineId;

    @Column(name = "extension_type", nullable = false)
    private String extensionType;

    @Column(name = "months_added", nullable = false)
    private Integer monthsAdded;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private String status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "remarks")
    private String remarks;
}
