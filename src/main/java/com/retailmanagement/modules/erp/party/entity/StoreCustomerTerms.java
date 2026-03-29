package com.retailmanagement.modules.erp.party.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "ErpStoreCustomerTerms")
@Table(name = "store_customer_terms")
public class StoreCustomerTerms extends ErpOrgScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_segment", nullable = false)
    private String customerSegment;

    @Column(name = "credit_limit", nullable = false, precision = 18, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "credit_days")
    private Integer creditDays;

    @Column(name = "loyalty_enabled", nullable = false)
    private Boolean loyaltyEnabled = false;

    @Column(name = "loyalty_points_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal loyaltyPointsBalance = BigDecimal.ZERO;

    @Column(name = "price_tier")
    private String priceTier;

    @Column(name = "discount_policy")
    private String discountPolicy;

    @Column(name = "is_preferred", nullable = false)
    private Boolean isPreferred = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    @Column(name = "remarks")
    private String remarks;
}
