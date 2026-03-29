package com.retailmanagement.modules.erp.subscription.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "subscription_feature")
public class SubscriptionFeature extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "module_code", nullable = false)
    private String moduleCode;

    @Column
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
