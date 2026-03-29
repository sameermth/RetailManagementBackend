package com.retailmanagement.modules.erp.tax.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tax_registration")
public class TaxRegistration extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "registration_type", nullable = false)
    private String registrationType;

    @Column(name = "registration_name", nullable = false)
    private String registrationName;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "gstin", nullable = false)
    private String gstin;

    @Column(name = "registration_state_code", nullable = false)
    private String registrationStateCode;

    @Column(name = "registration_state_name")
    private String registrationStateName;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
