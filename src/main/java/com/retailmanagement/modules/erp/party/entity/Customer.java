package com.retailmanagement.modules.erp.party.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity(name="ErpCustomer") @Table(name="customer")
public class Customer extends ErpOrgBranchScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="organization_person_profile_id") private Long organizationPersonProfileId;
 @Column(name="linked_organization_id") private Long linkedOrganizationId;
 @Column(name="customer_code", nullable=false) private String customerCode;
 @Column(name="full_name", nullable=false) private String fullName;
 @Column(name="customer_type", nullable=false) private String customerType;
 @Column(name="legal_name", nullable=false) private String legalName;
 @Column(name="trade_name", nullable=false) private String tradeName;
 private String phone; private String email; private String gstin;
 @Column(name="billing_address") private String billingAddress;
 @Column(name="shipping_address") private String shippingAddress;
 private String state;
 @Column(name="state_code") private String stateCode;
 @Column(name="contact_person_name") private String contactPersonName;
 @Column(name="contact_person_phone") private String contactPersonPhone;
 @Column(name="contact_person_email") private String contactPersonEmail;
 @Column(name="is_platform_linked", nullable = false) private Boolean isPlatformLinked = false;
 @Column(name="credit_limit", precision=18, scale=2) private BigDecimal creditLimit;
 private String notes;
 @Column(nullable=false) private String status;
}
