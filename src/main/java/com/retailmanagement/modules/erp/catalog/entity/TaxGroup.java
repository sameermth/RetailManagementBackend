package com.retailmanagement.modules.erp.catalog.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter; import lombok.Setter;

@Getter @Setter @Entity @Table(name="tax_group")
public class TaxGroup extends ErpOrgScopedEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String name;
 @Column(nullable=false) private String code;
 @Column(name="cgst_rate", nullable=false, precision=9, scale=4) private BigDecimal cgstRate = BigDecimal.ZERO;
 @Column(name="sgst_rate", nullable=false, precision=9, scale=4) private BigDecimal sgstRate = BigDecimal.ZERO;
 @Column(name="igst_rate", nullable=false, precision=9, scale=4) private BigDecimal igstRate = BigDecimal.ZERO;
 @Column(name="cess_rate", nullable=false, precision=9, scale=4) private BigDecimal cessRate = BigDecimal.ZERO;
 @Column(name="is_active", nullable=false) private Boolean isActive = true;
}
