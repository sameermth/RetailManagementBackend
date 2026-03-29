package com.retailmanagement.modules.erp.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class BankReconciliationResponses {
    private BankReconciliationResponses() {}

    public record BankStatementEntryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long accountId,
            LocalDate entryDate,
            LocalDate valueDate,
            String referenceNumber,
            String description,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            BigDecimal signedAmount,
            String status,
            Long matchedLedgerEntryId,
            LocalDateTime matchedOn,
            Long matchedBy,
            String remarks
    ) {}

    public record BankReconciliationCandidateResponse(
            Long ledgerEntryId,
            Long voucherId,
            String voucherNumber,
            String voucherType,
            LocalDate entryDate,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            BigDecimal signedAmount,
            String narrative,
            boolean exactAmountMatch,
            long dayDifference
    ) {}

    public record BankReconciliationSummaryResponse(
            Long accountId,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal statementBalance,
            BigDecimal reconciledStatementBalance,
            BigDecimal bookBalance,
            BigDecimal reconciledBookBalance,
            long unmatchedCount,
            long reconciledCount,
            List<BankStatementEntryResponse> entries
    ) {}
}
