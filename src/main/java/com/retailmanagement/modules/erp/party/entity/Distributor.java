package com.retailmanagement.modules.erp.party.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "ErpDistributor")
@Table(name = "distributor")
public class Distributor extends ErpOrgBranchScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_person_profile_id")
    private Long organizationPersonProfileId;

    @Column(name = "distributor_code")
    private String distributorCode;

    @Column(nullable = false)
    private String name;

    private String phone;
    private String email;
    private String gstin;

    @Column(nullable = false)
    private String status;
}
