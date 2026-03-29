package com.retailmanagement.modules.erp.finance.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "voucher", indexes = {
        @Index(name = "idx_voucher_org_date", columnList = "organization_id,voucher_date"),
        @Index(name = "idx_voucher_reference", columnList = "reference_type,reference_id")
})
public class Voucher extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_number", nullable = false, length = 80)
    private String voucherNumber;

    @Column(name = "voucher_date", nullable = false)
    private LocalDate voucherDate;

    @Column(name = "voucher_type", nullable = false, length = 30)
    private String voucherType;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(nullable = false, length = 20)
    private String status = "POSTED";
}
