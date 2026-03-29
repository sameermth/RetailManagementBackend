package com.retailmanagement.modules.erp.party.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "ErpStoreSupplierTerms")
@Table(name = "store_supplier_terms", indexes = {
        @Index(name = "idx_erp_store_supplier_terms_supplier", columnList = "organization_id,supplier_id,is_active")
})
public class StoreSupplierTerms extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "payment_terms")
    private String paymentTerms;

    @Column(name = "credit_limit", nullable = false, precision = 18, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "credit_days")
    private Integer creditDays;

    @Column(name = "is_preferred", nullable = false)
    private Boolean isPreferred = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    @Column(name = "order_via_email", nullable = false)
    private Boolean orderViaEmail = false;

    @Column(name = "order_via_whatsapp", nullable = false)
    private Boolean orderViaWhatsapp = false;

    @Column(name = "remarks")
    private String remarks;
}
