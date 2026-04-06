package com.retailmanagement.modules.erp.service.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "service_agreement_item", indexes = {
        @Index(name = "idx_service_agreement_item_agreement", columnList = "service_agreement_id"),
        @Index(name = "idx_service_agreement_item_ownership", columnList = "product_ownership_id"),
        @Index(name = "idx_service_agreement_item_invoice_line", columnList = "sales_invoice_line_id")
})
public class ServiceAgreementItem extends ErpAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_agreement_id", nullable = false)
    private Long serviceAgreementId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_ownership_id")
    private Long productOwnershipId;

    @Column(name = "sales_invoice_line_id")
    private Long salesInvoiceLineId;

    @Column(name = "serial_number_id")
    private Long serialNumberId;

    @Column(name = "coverage_scope", nullable = false)
    private String coverageScope;

    @Column(name = "included_service_notes")
    private String includedServiceNotes;
}
