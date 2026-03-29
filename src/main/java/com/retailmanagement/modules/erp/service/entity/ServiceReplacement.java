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
@Table(name = "service_replacement", indexes = {
        @Index(name = "idx_service_replacement_ticket", columnList = "service_ticket_id"),
        @Index(name = "idx_service_replacement_claim", columnList = "warranty_claim_id"),
        @Index(name = "idx_service_replacement_customer", columnList = "customer_id,issued_on")
})
public class ServiceReplacement extends ErpOrgBranchScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "service_ticket_id")
    private Long serviceTicketId;

    @Column(name = "warranty_claim_id")
    private Long warrantyClaimId;

    @Column(name = "sales_return_id")
    private Long salesReturnId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "original_product_id", nullable = false)
    private Long originalProductId;

    @Column(name = "original_serial_number_id")
    private Long originalSerialNumberId;

    @Column(name = "original_product_ownership_id")
    private Long originalProductOwnershipId;

    @Column(name = "replacement_product_id", nullable = false)
    private Long replacementProductId;

    @Column(name = "replacement_serial_number_id")
    private Long replacementSerialNumberId;

    @Column(name = "replacement_uom_id", nullable = false)
    private Long replacementUomId;

    @Column(name = "replacement_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal replacementQuantity;

    @Column(name = "replacement_base_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal replacementBaseQuantity;

    @Column(name = "replacement_number", nullable = false)
    private String replacementNumber;

    @Column(name = "replacement_type", nullable = false)
    private String replacementType;

    @Column(name = "stock_source_bucket", nullable = false)
    private String stockSourceBucket;

    @Column(nullable = false)
    private String status;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "notes")
    private String notes;
}
