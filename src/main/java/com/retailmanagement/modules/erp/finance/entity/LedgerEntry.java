package com.retailmanagement.modules.erp.finance.entity;

import com.retailmanagement.modules.erp.common.model.ErpOrgBranchScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ledger_entry", indexes = {
        @Index(name = "idx_ledger_org_date", columnList = "organization_id,entry_date"),
        @Index(name = "idx_ledger_account_date", columnList = "account_id,entry_date"),
        @Index(name = "idx_ledger_customer", columnList = "customer_id,entry_date"),
        @Index(name = "idx_ledger_supplier", columnList = "supplier_id,entry_date"),
        @Index(name = "idx_ledger_voucher", columnList = "voucher_id")
})
public class LedgerEntry extends ErpOrgBranchScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "debit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String narrative;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "sales_invoice_id")
    private Long salesInvoiceId;

    @Column(name = "purchase_receipt_id")
    private Long purchaseReceiptId;

    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "service_replacement_id")
    private Long serviceReplacementId;
}
