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
@Table(name = "bank_statement_import_batch", indexes = {
        @Index(name = "idx_bank_statement_import_batch_org_account", columnList = "organization_id,account_id,imported_at")
})
public class BankStatementImportBatch extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "import_reference", nullable = false, length = 80, unique = true)
    private String importReference;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType = "MANUAL";

    @Column(name = "source_reference", length = 120)
    private String sourceReference;

    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    @Column(name = "statement_from_date")
    private LocalDate statementFromDate;

    @Column(name = "statement_to_date")
    private LocalDate statementToDate;

    @Column(name = "entry_count", nullable = false)
    private Integer entryCount = 0;

    @Column(name = "total_debit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDebitAmount = BigDecimal.ZERO;

    @Column(name = "total_credit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCreditAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    private String status = "IMPORTED";

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "imported_by")
    private Long importedBy;

    @Column(columnDefinition = "text")
    private String remarks;
}
