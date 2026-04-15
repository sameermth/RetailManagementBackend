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
@Table(name = "purchase_order_supplier_access", indexes = {
        @Index(name = "idx_purchase_order_supplier_access_order", columnList = "purchase_order_id"),
        @Index(name = "idx_purchase_order_supplier_access_token", columnList = "access_token")
})
public class PurchaseOrderSupplierAccess extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "access_token", nullable = false, length = 120, unique = true)
    private String accessToken;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;
}
