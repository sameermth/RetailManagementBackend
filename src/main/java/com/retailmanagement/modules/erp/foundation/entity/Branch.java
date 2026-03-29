package com.retailmanagement.modules.erp.foundation.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="branch", indexes={@Index(name="idx_branch_org_code", columnList="organization_id,code", unique=true)})
public class Branch extends ErpOrgScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String name;
 @Column(nullable=false) private String code;
 @Column(name="phone") private String phone;
 @Column(name="email") private String email;
 @Column(name="address_line1") private String addressLine1;
 @Column(name="address_line2") private String addressLine2;
 @Column(name="city") private String city;
 @Column(name="state") private String state;
 @Column(name="postal_code") private String postalCode;
 @Column(name="country") private String country;
 @Column(name="is_active", nullable=false) private Boolean isActive = true;
}
