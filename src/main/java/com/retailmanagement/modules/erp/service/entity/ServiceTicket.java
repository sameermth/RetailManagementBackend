package com.retailmanagement.modules.erp.service.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "service_ticket", indexes = {
        @Index(name = "idx_service_ticket_org_status", columnList = "organization_id,status"),
        @Index(name = "idx_service_ticket_customer", columnList = "customer_id"),
        @Index(name = "idx_service_ticket_invoice", columnList = "sales_invoice_id")
})
public class ServiceTicket extends ErpOrgBranchScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "sales_invoice_id")
    private Long salesInvoiceId;

    @Column(name = "sales_return_id")
    private Long salesReturnId;

    @Column(name = "ticket_number", nullable = false)
    private String ticketNumber;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String status;

    @Column(name = "complaint_summary", nullable = false)
    private String complaintSummary;

    @Column(name = "issue_description")
    private String issueDescription;

    @Column(name = "reported_on", nullable = false)
    private LocalDate reportedOn;

    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;
}
