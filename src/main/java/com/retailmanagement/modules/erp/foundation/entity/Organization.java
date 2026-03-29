package com.retailmanagement.modules.erp.foundation.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="organization")
public class Organization extends ErpAuditableEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String name;
 @Column(nullable=false, unique=true) private String code;
 @Column(name="legal_name") private String legalName;
 @Column(name="phone") private String phone;
 @Column(name="email") private String email;
 @Column(name="gstin") private String gstin;
 @Column(name="owner_account_id", nullable=false) private Long ownerAccountId;
 @Column(name="gst_threshold_amount", nullable=false, precision=18, scale=2) private BigDecimal gstThresholdAmount = new BigDecimal("4000000.00");
 @Column(name="gst_threshold_alert_enabled", nullable=false) private Boolean gstThresholdAlertEnabled = true;
 @Column(name="subscription_version", nullable=false) private Long subscriptionVersion = 1L;
 @Column(name="is_active", nullable=false) private Boolean isActive = true;
}
