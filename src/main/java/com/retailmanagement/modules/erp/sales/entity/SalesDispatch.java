package com.retailmanagement.modules.erp.sales.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sales_dispatch", indexes = {
        @Index(name = "idx_sales_dispatch_org", columnList = "organization_id,dispatch_date"),
        @Index(name = "idx_sales_dispatch_invoice", columnList = "sales_invoice_id,status")
})
public class SalesDispatch extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_invoice_id", nullable = false)
    private Long salesInvoiceId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "dispatch_number", nullable = false, unique = true)
    private String dispatchNumber;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(nullable = false)
    private String status;

    @Column(name = "transporter_name")
    private String transporterName;

    @Column(name = "transporter_id")
    private String transporterId;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column
    private String remarks;

    @Column(name = "packed_at")
    private LocalDateTime packedAt;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;
}
