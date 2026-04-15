package com.retailmanagement.modules.erp.finance.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_statement_entry", indexes = {
        @Index(name = "idx_bank_statement_entry_org_account_date", columnList = "organization_id,account_id,entry_date")
})
public class BankStatementEntry extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "import_batch_id")
    private Long importBatchId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "reference_number", length = 120)
    private String referenceNumber;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "debit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "UNMATCHED";

    @Column(name = "matched_ledger_entry_id")
    private Long matchedLedgerEntryId;

    @Column(name = "matched_on")
    private LocalDateTime matchedOn;

    @Column(name = "matched_by")
    private Long matchedBy;

    @Column(columnDefinition = "text")
    private String remarks;
}
