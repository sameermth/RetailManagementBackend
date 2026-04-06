package com.retailmanagement.modules.erp.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ErpFinanceDtos {
    private ErpFinanceDtos() {}

    public record CreateAccountRequest(
            Long organizationId,
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String accountType,
            Long parentAccountId,
            Boolean isSystem,
            Boolean isActive
    ) {}

    public record UpdateAccountRequest(
            Long organizationId,
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String accountType,
            Long parentAccountId,
            Boolean isActive
    ) {}

    public record CreateVoucherRequest(
            Long organizationId,
            Long branchId,
            LocalDate voucherDate,
            @NotBlank String voucherType,
            String referenceType,
            Long referenceId,
            String remarks,
            @NotEmpty List<@Valid CreateVoucherLineRequest> lines
    ) {}

    public record CreateVoucherLineRequest(
            @NotNull Long accountId,
            @DecimalMin(value = "0.00") BigDecimal debitAmount,
            @DecimalMin(value = "0.00") BigDecimal creditAmount,
            String narrative,
            Long customerId,
            Long supplierId,
            Long salesInvoiceId,
            Long purchaseReceiptId
    ) {}

    public record PartyLedgerQuery(
            @NotBlank String partyType,
            @NotNull Long partyId,
            @NotNull LocalDate fromDate,
            @NotNull LocalDate toDate
    ) {}

    public record AccountLedgerQuery(
            @NotNull Long accountId,
            @NotNull LocalDate fromDate,
            @NotNull LocalDate toDate
    ) {}

    public record OutstandingQuery(
            @NotBlank String partyType,
            Long partyId,
            LocalDate asOfDate
    ) {}

    public record AdjustmentReviewQuery(
            LocalDate fromDate,
            LocalDate toDate
    ) {}

    public record CashBankSummaryQuery(
            LocalDate fromDate,
            LocalDate toDate
    ) {}

    public record ExpenseSummaryQuery(
            LocalDate fromDate,
            LocalDate toDate
    ) {}

    public record AccountResponse(
            Long id,
            Long organizationId,
            String code,
            String name,
            String accountType,
            Long parentAccountId,
            Boolean isSystem,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record VoucherResponse(
            Long id,
            Long organizationId,
            Long branchId,
            String voucherNumber,
            LocalDate voucherDate,
            String voucherType,
            String referenceType,
            Long referenceId,
            String remarks,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record LedgerEntryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long voucherId,
            String voucherNumber,
            String voucherType,
            String voucherReferenceType,
            Long voucherReferenceId,
            Long accountId,
            LocalDate entryDate,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            String narrative,
            Long customerId,
            Long supplierId,
            Long salesInvoiceId,
            Long purchaseReceiptId,
            Long expenseId,
            Long serviceReplacementId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record VoucherDetailsResponse(
            VoucherResponse voucher,
            List<LedgerEntryResponse> entries
    ) {}

    public record PartyLedgerDetailsResponse(
            String partyType,
            Long partyId,
            List<LedgerEntryResponse> entries,
            BigDecimal totalDebit,
            BigDecimal totalCredit
    ) {}

    public record AccountLedgerDetailsResponse(
            Long accountId,
            String accountCode,
            String accountName,
            String accountType,
            List<LedgerEntryResponse> entries,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            BigDecimal netMovement
    ) {}

    public record DocumentOutstandingResponse(
            String partyType,
            Long partyId,
            Long documentId,
            String documentNumber,
            LocalDate documentDate,
            LocalDate dueDate,
            BigDecimal totalAmount,
            BigDecimal allocatedAmount,
            BigDecimal outstandingAmount,
            String agingBucket,
            List<AllocationReferenceResponse> allocations
    ) {}

    public record AllocationReferenceResponse(
            String allocationType,
            Long allocationId,
            String allocationNumber,
            LocalDate allocationDate,
            BigDecimal allocatedAmount
    ) {}

    public record AgingBucketsResponse(
            BigDecimal current,
            BigDecimal bucket1To30,
            BigDecimal bucket31To60,
            BigDecimal bucket61To90,
            BigDecimal bucket90Plus
    ) {}

    public record OutstandingSummaryResponse(
            String partyType,
            Long partyId,
            LocalDate asOfDate,
            BigDecimal totalOutstanding,
            List<DocumentOutstandingResponse> documents,
            AgingBucketsResponse aging
    ) {}

    public record AdjustmentReviewDocumentResponse(
            String documentType,
            Long documentId,
            String documentNumber,
            LocalDate documentDate,
            String status,
            String partyType,
            Long partyId,
            String linkedDocumentType,
            Long linkedDocumentId,
            String adjustmentType,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            Long voucherId,
            String voucherNumber,
            String voucherType,
            String voucherReferenceType,
            Long voucherReferenceId,
            String remarks
    ) {}

    public record AdjustmentReviewSummaryResponse(
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal salesReturnTotal,
            BigDecimal purchaseReturnTotal,
            BigDecimal replacementTotal,
            List<AdjustmentReviewDocumentResponse> documents
    ) {}

    public record CashBankAccountSummaryResponse(
            Long accountId,
            String accountCode,
            String accountName,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            BigDecimal inflow,
            BigDecimal outflow,
            BigDecimal netMovement
    ) {}

    public record CashBankSummaryResponse(
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal totalInflow,
            BigDecimal totalOutflow,
            BigDecimal netMovement,
            List<CashBankAccountSummaryResponse> accounts
    ) {}

    public record ExpenseCategorySummaryResponse(
            Long expenseCategoryId,
            String expenseCategoryCode,
            String expenseCategoryName,
            BigDecimal totalAmount,
            Long expenseCount
    ) {}

    public record ExpenseSummaryResponse(
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal totalExpenseAmount,
            BigDecimal paidExpenseAmount,
            BigDecimal approvedUnpaidExpenseAmount,
            BigDecimal cancelledExpenseAmount,
            List<ExpenseCategorySummaryResponse> categories
    ) {}
}
