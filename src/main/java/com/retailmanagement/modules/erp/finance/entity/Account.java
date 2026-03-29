package com.retailmanagement.modules.erp.finance.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "account", indexes = {
        @Index(name = "idx_account_org_type", columnList = "organization_id,account_type"),
        @Index(name = "idx_account_org_name", columnList = "organization_id,name")
})
public class Account extends ErpOrgScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "account_type", nullable = false, length = 30)
    private String accountType;

    @Column(name = "parent_account_id")
    private Long parentAccountId;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
