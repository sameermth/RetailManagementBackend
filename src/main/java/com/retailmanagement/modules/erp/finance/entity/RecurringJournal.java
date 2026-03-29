package com.retailmanagement.modules.erp.finance.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "recurring_journal", indexes = {
        @Index(name = "idx_recurring_journal_org_next_run", columnList = "organization_id,is_active,next_run_date")
})
public class RecurringJournal extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_number", nullable = false, length = 80)
    private String templateNumber;

    @Column(name = "voucher_type", nullable = false, length = 30)
    private String voucherType;

    @Column(name = "frequency", nullable = false, length = 20)
    private String frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_voucher_id")
    private Long lastVoucherId;
}
