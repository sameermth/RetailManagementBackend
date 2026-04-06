package com.retailmanagement.modules.erp.service.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "service_agreement", indexes = {
        @Index(name = "idx_service_agreement_org_status", columnList = "organization_id,status"),
        @Index(name = "idx_service_agreement_invoice", columnList = "sales_invoice_id")
})
public class ServiceAgreement extends ErpOrgBranchScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "sales_invoice_id", nullable = false)
    private Long salesInvoiceId;

    @Column(name = "agreement_number", nullable = false)
    private String agreementNumber;

    @Column(name = "agreement_type", nullable = false)
    private String agreementType;

    @Column(nullable = false)
    private String status;

    @Column(name = "service_start_date", nullable = false)
    private LocalDate serviceStartDate;

    @Column(name = "service_end_date", nullable = false)
    private LocalDate serviceEndDate;

    @Column(name = "labor_included", nullable = false)
    private Boolean laborIncluded;

    @Column(name = "parts_included", nullable = false)
    private Boolean partsIncluded;

    @Column(name = "preventive_visits_included")
    private Integer preventiveVisitsIncluded;

    @Column(name = "visit_limit")
    private Integer visitLimit;

    @Column(name = "sla_hours")
    private Integer slaHours;

    @Column(name = "agreement_amount")
    private BigDecimal agreementAmount;

    @Column(name = "notes")
    private String notes;
}
