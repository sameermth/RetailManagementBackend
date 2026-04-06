package com.retailmanagement.modules.erp.finance.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.finance.dto.ErpFinanceDtos;
import com.retailmanagement.modules.erp.finance.entity.Account;
import com.retailmanagement.modules.erp.finance.entity.LedgerEntry;
import com.retailmanagement.modules.erp.finance.entity.Voucher;
import com.retailmanagement.modules.erp.finance.service.ErpFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/finance")
@RequiredArgsConstructor
@Tag(name = "ERP Finance", description = "ERP accounts, vouchers, and ledger reporting endpoints")
public class ErpFinanceController {

    private final ErpFinanceService erpFinanceService;

    @GetMapping("/accounts")
    @Operation(summary = "List chart of accounts")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<List<ErpFinanceDtos.AccountResponse>> listAccounts(@RequestParam Long organizationId,
                                                                             @RequestParam(required = false) String accountType,
                                                                             @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ErpApiResponse.ok(erpFinanceService.listAccounts(organizationId, accountType, includeInactive).stream().map(this::toAccountResponse).toList());
    }

    @GetMapping("/accounts/{id}")
    @Operation(summary = "Get account details")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<ErpFinanceDtos.AccountResponse> getAccount(@PathVariable Long id,
                                                                     @RequestParam Long organizationId) {
        return ErpApiResponse.ok(toAccountResponse(erpFinanceService.getAccount(organizationId, id)));
    }

    @PostMapping("/accounts")
    @Operation(summary = "Create account")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<ErpFinanceDtos.AccountResponse> createAccount(@RequestBody @Valid ErpFinanceDtos.CreateAccountRequest request) {
        return ErpApiResponse.ok(toAccountResponse(erpFinanceService.createAccount(request)), "Account created");
    }

    @PutMapping("/accounts/{id}")
    @Operation(summary = "Update account")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<ErpFinanceDtos.AccountResponse> updateAccount(@PathVariable Long id,
                                                                        @RequestBody @Valid ErpFinanceDtos.UpdateAccountRequest request) {
        return ErpApiResponse.ok(toAccountResponse(erpFinanceService.updateAccount(id, request)), "Account updated");
    }

    @DeleteMapping("/accounts/{id}")
    @Operation(summary = "Delete account")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<Void> deleteAccount(@PathVariable Long id,
                                              @RequestParam Long organizationId) {
        erpFinanceService.deleteAccount(organizationId, id);
        return ErpApiResponse.ok(null, "Account deleted");
    }

    @GetMapping("/vouchers")
    @Operation(summary = "List vouchers")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<List<ErpFinanceDtos.VoucherResponse>> listVouchers(@RequestParam Long organizationId) {
        return ErpApiResponse.ok(erpFinanceService.listVouchers(organizationId).stream().map(this::toVoucherResponse).toList());
    }

    @GetMapping("/vouchers/{id}")
    @Operation(summary = "Get voucher details")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<ErpFinanceDtos.VoucherDetailsResponse> getVoucher(@PathVariable Long id,
                                                                            @RequestParam Long organizationId) {
        return ErpApiResponse.ok(toVoucherDetailsResponse(erpFinanceService.getVoucher(organizationId, id)));
    }

    @PostMapping("/vouchers")
    @Operation(summary = "Create voucher")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<ErpFinanceDtos.VoucherResponse> createVoucher(@RequestBody @Valid ErpFinanceDtos.CreateVoucherRequest request) {
        return ErpApiResponse.ok(toVoucherResponse(erpFinanceService.createVoucher(request)), "Voucher posted");
    }

    @GetMapping("/daybook")
    @Operation(summary = "Get daybook entries")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<List<ErpFinanceDtos.LedgerEntryResponse>> daybook(@RequestParam Long organizationId,
                                                                            @RequestParam(required = false) LocalDate fromDate,
                                                                            @RequestParam(required = false) LocalDate toDate) {
        Map<Long, Voucher> voucherById = new HashMap<>();
        return ErpApiResponse.ok(erpFinanceService.daybook(organizationId, fromDate, toDate).stream()
                .map(entry -> toLedgerEntryResponse(entry, voucherById))
                .toList());
    }

    @PostMapping("/party-ledger")
    @Operation(summary = "Get party ledger")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<ErpFinanceDtos.PartyLedgerDetailsResponse> partyLedger(@RequestParam Long organizationId,
                                                                                 @RequestBody @Valid ErpFinanceDtos.PartyLedgerQuery query) {
        return ErpApiResponse.ok(toPartyLedgerDetailsResponse(erpFinanceService.partyLedger(organizationId, query)));
    }

    @PostMapping("/account-ledger")
    @Operation(summary = "Get account ledger")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<ErpFinanceDtos.AccountLedgerDetailsResponse> accountLedger(@RequestParam Long organizationId,
                                                                                     @RequestBody @Valid ErpFinanceDtos.AccountLedgerQuery query) {
        return ErpApiResponse.ok(toAccountLedgerDetailsResponse(erpFinanceService.accountLedger(organizationId, query)));
    }

    @PostMapping("/outstanding")
    @Operation(summary = "Get outstanding summary")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<ErpFinanceDtos.OutstandingSummaryResponse> outstanding(@RequestParam Long organizationId,
                                                                                 @RequestBody @Valid ErpFinanceDtos.OutstandingQuery query) {
        return ErpApiResponse.ok(toOutstandingSummaryResponse(erpFinanceService.outstanding(organizationId, query)));
    }

    @PostMapping("/adjustments/review")
    @Operation(summary = "Review returns and replacement finance adjustments")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<ErpFinanceDtos.AdjustmentReviewSummaryResponse> adjustmentReview(@RequestParam Long organizationId,
                                                                                           @RequestBody(required = false) ErpFinanceDtos.AdjustmentReviewQuery query) {
        ErpFinanceDtos.AdjustmentReviewQuery effectiveQuery = query == null
                ? new ErpFinanceDtos.AdjustmentReviewQuery(null, null)
                : query;
        return ErpApiResponse.ok(toAdjustmentReviewSummaryResponse(erpFinanceService.adjustmentReview(organizationId, effectiveQuery)));
    }

    @PostMapping("/cash-bank-summary")
    @Operation(summary = "Get cash and bank movement summary")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<ErpFinanceDtos.CashBankSummaryResponse> cashBankSummary(@RequestParam Long organizationId,
                                                                                  @RequestBody(required = false) ErpFinanceDtos.CashBankSummaryQuery query) {
        ErpFinanceDtos.CashBankSummaryQuery effectiveQuery = query == null
                ? new ErpFinanceDtos.CashBankSummaryQuery(null, null)
                : query;
        return ErpApiResponse.ok(toCashBankSummaryResponse(erpFinanceService.cashBankSummary(organizationId, effectiveQuery)));
    }

    @PostMapping("/expense-summary")
    @Operation(summary = "Get expense summary and category breakdown")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<ErpFinanceDtos.ExpenseSummaryResponse> expenseSummary(@RequestParam Long organizationId,
                                                                                @RequestBody(required = false) ErpFinanceDtos.ExpenseSummaryQuery query) {
        ErpFinanceDtos.ExpenseSummaryQuery effectiveQuery = query == null
                ? new ErpFinanceDtos.ExpenseSummaryQuery(null, null)
                : query;
        return ErpApiResponse.ok(toExpenseSummaryResponse(erpFinanceService.expenseSummary(organizationId, effectiveQuery)));
    }

    private ErpFinanceDtos.AccountResponse toAccountResponse(Account account) {
        return new ErpFinanceDtos.AccountResponse(account.getId(), account.getOrganizationId(), account.getCode(), account.getName(),
                account.getAccountType(), account.getParentAccountId(), account.getIsSystem(), account.getIsActive(),
                account.getCreatedAt(), account.getUpdatedAt());
    }

    private ErpFinanceDtos.VoucherResponse toVoucherResponse(Voucher voucher) {
        return new ErpFinanceDtos.VoucherResponse(voucher.getId(), voucher.getOrganizationId(), voucher.getBranchId(),
                voucher.getVoucherNumber(), voucher.getVoucherDate(), voucher.getVoucherType(), voucher.getReferenceType(),
                voucher.getReferenceId(), voucher.getRemarks(), voucher.getStatus(), voucher.getCreatedAt(), voucher.getUpdatedAt());
    }

    private ErpFinanceDtos.LedgerEntryResponse toLedgerEntryResponse(LedgerEntry entry, Map<Long, Voucher> voucherById) {
        Voucher voucher = voucherById.computeIfAbsent(entry.getVoucherId(), id ->
                erpFinanceService.getVoucher(entry.getOrganizationId(), id).voucher());
        return new ErpFinanceDtos.LedgerEntryResponse(entry.getId(), entry.getOrganizationId(), entry.getBranchId(),
                entry.getVoucherId(), voucher.getVoucherNumber(), voucher.getVoucherType(), voucher.getReferenceType(), voucher.getReferenceId(),
                entry.getAccountId(), entry.getEntryDate(), entry.getDebitAmount(), entry.getCreditAmount(),
                entry.getNarrative(), entry.getCustomerId(), entry.getSupplierId(), entry.getSalesInvoiceId(),
                entry.getPurchaseReceiptId(), entry.getExpenseId(), entry.getServiceReplacementId(), entry.getCreatedAt(), entry.getUpdatedAt());
    }

    private ErpFinanceDtos.VoucherDetailsResponse toVoucherDetailsResponse(ErpFinanceService.VoucherDetails details) {
        Map<Long, Voucher> voucherById = new HashMap<>();
        return new ErpFinanceDtos.VoucherDetailsResponse(
                toVoucherResponse(details.voucher()),
                details.entries().stream().map(entry -> toLedgerEntryResponse(entry, voucherById)).toList()
        );
    }

    private ErpFinanceDtos.PartyLedgerDetailsResponse toPartyLedgerDetailsResponse(ErpFinanceService.PartyLedgerDetails details) {
        Map<Long, Voucher> voucherById = new HashMap<>();
        return new ErpFinanceDtos.PartyLedgerDetailsResponse(
                details.partyType(),
                details.partyId(),
                details.entries().stream().map(entry -> toLedgerEntryResponse(entry, voucherById)).toList(),
                details.totalDebit(),
                details.totalCredit()
        );
    }

    private ErpFinanceDtos.AccountLedgerDetailsResponse toAccountLedgerDetailsResponse(ErpFinanceService.AccountLedgerDetails details) {
        Map<Long, Voucher> voucherById = new HashMap<>();
        return new ErpFinanceDtos.AccountLedgerDetailsResponse(
                details.account().getId(),
                details.account().getCode(),
                details.account().getName(),
                details.account().getAccountType(),
                details.entries().stream().map(entry -> toLedgerEntryResponse(entry, voucherById)).toList(),
                details.totalDebit(),
                details.totalCredit(),
                details.netMovement()
        );
    }

    private ErpFinanceDtos.OutstandingSummaryResponse toOutstandingSummaryResponse(ErpFinanceService.OutstandingSummary summary) {
        return new ErpFinanceDtos.OutstandingSummaryResponse(
                summary.partyType(),
                summary.partyId(),
                summary.asOfDate(),
                summary.totalOutstanding(),
                summary.documents().stream()
                        .map(document -> new ErpFinanceDtos.DocumentOutstandingResponse(
                                document.partyType(),
                                document.partyId(),
                                document.documentId(),
                                document.documentNumber(),
                                document.documentDate(),
                                document.dueDate(),
                                document.totalAmount(),
                                document.allocatedAmount(),
                                document.outstandingAmount(),
                                document.agingBucket(),
                                document.allocations().stream()
                                        .map(allocation -> new ErpFinanceDtos.AllocationReferenceResponse(
                                                allocation.allocationType(),
                                                allocation.allocationId(),
                                                allocation.allocationNumber(),
                                                allocation.allocationDate(),
                                                allocation.allocatedAmount()
                                        )).toList()
                        )).toList(),
                new ErpFinanceDtos.AgingBucketsResponse(
                        summary.aging().current(),
                        summary.aging().bucket1To30(),
                        summary.aging().bucket31To60(),
                        summary.aging().bucket61To90(),
                        summary.aging().bucket90Plus()
                )
        );
    }

    private ErpFinanceDtos.AdjustmentReviewSummaryResponse toAdjustmentReviewSummaryResponse(ErpFinanceService.AdjustmentReviewSummary summary) {
        return new ErpFinanceDtos.AdjustmentReviewSummaryResponse(
                summary.fromDate(),
                summary.toDate(),
                summary.salesReturnTotal(),
                summary.purchaseReturnTotal(),
                summary.replacementTotal(),
                summary.documents().stream()
                        .map(document -> new ErpFinanceDtos.AdjustmentReviewDocumentResponse(
                                document.documentType(),
                                document.documentId(),
                                document.documentNumber(),
                                document.documentDate(),
                                document.status(),
                                document.partyType(),
                                document.partyId(),
                                document.linkedDocumentType(),
                                document.linkedDocumentId(),
                                document.adjustmentType(),
                                document.subtotal(),
                                document.taxAmount(),
                                document.totalAmount(),
                                document.voucher() == null ? null : document.voucher().getId(),
                                document.voucher() == null ? null : document.voucher().getVoucherNumber(),
                                document.voucher() == null ? null : document.voucher().getVoucherType(),
                                document.voucher() == null ? null : document.voucher().getReferenceType(),
                                document.voucher() == null ? null : document.voucher().getReferenceId(),
                                document.voucher() == null ? null : document.voucher().getRemarks()
                        )).toList()
        );
    }

    private ErpFinanceDtos.CashBankSummaryResponse toCashBankSummaryResponse(ErpFinanceService.CashBankSummary summary) {
        return new ErpFinanceDtos.CashBankSummaryResponse(
                summary.fromDate(),
                summary.toDate(),
                summary.totalInflow(),
                summary.totalOutflow(),
                summary.netMovement(),
                summary.accounts().stream()
                        .map(account -> new ErpFinanceDtos.CashBankAccountSummaryResponse(
                                account.accountId(),
                                account.accountCode(),
                                account.accountName(),
                                account.totalDebit(),
                                account.totalCredit(),
                                account.inflow(),
                                account.outflow(),
                                account.netMovement()
                        )).toList()
        );
    }

    private ErpFinanceDtos.ExpenseSummaryResponse toExpenseSummaryResponse(ErpFinanceService.ExpenseSummary summary) {
        return new ErpFinanceDtos.ExpenseSummaryResponse(
                summary.fromDate(),
                summary.toDate(),
                summary.totalExpenseAmount(),
                summary.paidExpenseAmount(),
                summary.approvedUnpaidExpenseAmount(),
                summary.cancelledExpenseAmount(),
                summary.categories().stream()
                        .map(category -> new ErpFinanceDtos.ExpenseCategorySummaryResponse(
                                category.expenseCategoryId(),
                                category.expenseCategoryCode(),
                                category.expenseCategoryName(),
                                category.totalAmount(),
                                category.expenseCount()
                        )).toList()
        );
    }
}
