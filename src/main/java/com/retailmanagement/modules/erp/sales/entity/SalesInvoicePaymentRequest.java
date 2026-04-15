package com.retailmanagement.modules.erp.sales.entity;

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
@Table(name = "sales_invoice_payment_request", indexes = {
        @Index(name = "idx_sales_invoice_payment_request_invoice", columnList = "sales_invoice_id,status"),
        @Index(name = "idx_sales_invoice_payment_request_org", columnList = "organization_id,request_date")
})
public class SalesInvoicePaymentRequest extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_invoice_id", nullable = false)
    private Long salesInvoiceId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "request_number", nullable = false, length = 80, unique = true)
    private String requestNumber;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "requested_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal requestedAmount = BigDecimal.ZERO;

    @Column(name = "provider_code", nullable = false, length = 40)
    private String providerCode = "MANUAL";

    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(name = "provider_status", length = 40)
    private String providerStatus;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel = "LINK";

    @Column(name = "payment_link_token", nullable = false, length = 120, unique = true)
    private String paymentLinkToken;

    @Column(name = "payment_link_url", length = 255)
    private String paymentLinkUrl;

    @Column(name = "provider_payload_json", columnDefinition = "text")
    private String providerPayloadJson;

    @Column(name = "provider_created_at")
    private LocalDateTime providerCreatedAt;

    @Column(name = "provider_last_synced_at")
    private LocalDateTime providerLastSyncedAt;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancel_reason", columnDefinition = "text")
    private String cancelReason;
}
