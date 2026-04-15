package com.retailmanagement.modules.erp.purchase.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "purchase_order_supplier_dispatch_notice", indexes = {
        @Index(name = "idx_po_supplier_dispatch_order", columnList = "purchase_order_id,dispatch_date"),
        @Index(name = "idx_po_supplier_dispatch_supplier", columnList = "supplier_id,dispatch_date")
})
public class PurchaseOrderSupplierDispatchNotice extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "dispatch_number", nullable = false, length = 80, unique = true)
    private String dispatchNumber;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "supplier_reference_number", length = 120)
    private String supplierReferenceNumber;

    @Column(name = "transporter_name", length = 120)
    private String transporterName;

    @Column(name = "vehicle_number", length = 60)
    private String vehicleNumber;

    @Column(name = "tracking_number", length = 120)
    private String trackingNumber;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
