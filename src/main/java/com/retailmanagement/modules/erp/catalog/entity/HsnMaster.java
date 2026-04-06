package com.retailmanagement.modules.erp.catalog.entity;

import com.retailmanagement.modules.erp.common.model.ErpAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hsn_master", indexes = {
        @Index(name = "idx_hsn_master_code", columnList = "hsn_code", unique = true),
        @Index(name = "idx_hsn_master_desc", columnList = "description")
})
public class HsnMaster extends ErpAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hsn_code", nullable = false)
    private String hsnCode;

    @Column(nullable = false)
    private String description;

    @Column(name = "chapter_code")
    private String chapterCode;

    @Column(name = "cgst_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal cgstRate = BigDecimal.ZERO;

    @Column(name = "sgst_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal sgstRate = BigDecimal.ZERO;

    @Column(name = "igst_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal igstRate = BigDecimal.ZERO;

    @Column(name = "cess_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal cessRate = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;
}
