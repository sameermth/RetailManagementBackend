package com.retailmanagement.modules.erp.party.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity(name="ErpSupplier") @Table(name="supplier")
public class Supplier extends ErpOrgBranchScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="organization_person_profile_id") private Long organizationPersonProfileId;
 @Column(name="linked_organization_id") private Long linkedOrganizationId;
 @Column(name="supplier_code", nullable=false) private String supplierCode;
 @Column(nullable=false) private String name;
 @Column(name="legal_name") private String legalName;
 @Column(name="trade_name") private String tradeName;
 private String phone; private String email; private String gstin;
 @Column(name="billing_address") private String billingAddress;
 @Column(name="shipping_address") private String shippingAddress;
 private String state;
 @Column(name="state_code") private String stateCode;
 @Column(name="contact_person_name") private String contactPersonName;
 @Column(name="contact_person_phone") private String contactPersonPhone;
 @Column(name="contact_person_email") private String contactPersonEmail;
 @Column(name="payment_terms") private String paymentTerms;
 @Column(name="is_platform_linked", nullable = false) private Boolean isPlatformLinked = false;
 private String notes;
 @Column(nullable=false) private String status;
}
