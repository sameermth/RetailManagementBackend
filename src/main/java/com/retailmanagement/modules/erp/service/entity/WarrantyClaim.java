package com.retailmanagement.modules.erp.service.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "warranty_claim", indexes = {
        @Index(name = "idx_warranty_claim_org_status", columnList = "organization_id,status"),
        @Index(name = "idx_warranty_claim_customer", columnList = "customer_id"),
        @Index(name = "idx_warranty_claim_ticket", columnList = "service_ticket_id")
})
public class WarrantyClaim extends ErpOrgBranchScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_ticket_id")
    private Long serviceTicketId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "serial_number_id")
    private Long serialNumberId;

    @Column(name = "product_ownership_id")
    private Long productOwnershipId;

    @Column(name = "sales_invoice_id")
    private Long salesInvoiceId;

    @Column(name = "sales_return_id")
    private Long salesReturnId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "distributor_id")
    private Long distributorId;

    @Column(name = "claim_number", nullable = false)
    private String claimNumber;

    @Column(name = "claim_type", nullable = false)
    private String claimType;

    @Column(nullable = false)
    private String status;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Column(name = "approved_on")
    private LocalDate approvedOn;

    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "claim_notes")
    private String claimNotes;

    @Column(name = "upstream_route_type")
    private String upstreamRouteType;

    @Column(name = "upstream_company_name")
    private String upstreamCompanyName;

    @Column(name = "upstream_reference_number")
    private String upstreamReferenceNumber;

    @Column(name = "upstream_status")
    private String upstreamStatus;

    @Column(name = "routed_on")
    private LocalDate routedOn;
}
