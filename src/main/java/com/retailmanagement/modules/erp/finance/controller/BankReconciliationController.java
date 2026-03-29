package com.retailmanagement.modules.erp.finance.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.finance.dto.BankReconciliationDtos;
import com.retailmanagement.modules.erp.finance.dto.BankReconciliationResponses;
import com.retailmanagement.modules.erp.finance.entity.BankStatementEntry;
import com.retailmanagement.modules.erp.finance.service.BankReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/finance/bank-reconciliation")
@RequiredArgsConstructor
@Tag(name = "ERP Finance", description = "Bank reconciliation endpoints")
public class BankReconciliationController {

    private final BankReconciliationService bankReconciliationService;

    @PostMapping("/statements")
    @Operation(summary = "Import bank statement entries")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<List<BankReconciliationResponses.BankStatementEntryResponse>> importStatement(
            @RequestBody @Valid BankReconciliationDtos.ImportBankStatementRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(
                bankReconciliationService.importStatement(orgId, branchId, request).stream().map(this::toResponse).toList(),
                "Bank statement entries imported"
        );
    }

    @PostMapping("/summary")
    @Operation(summary = "Get bank reconciliation summary")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<BankReconciliationResponses.BankReconciliationSummaryResponse> summary(
            @RequestParam(required = false) Long organizationId,
            @RequestBody @Valid BankReconciliationDtos.BankReconciliationQuery query) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(bankReconciliationService.summary(orgId, query));
    }

    @GetMapping("/statements/{id}/candidates")
    @Operation(summary = "Get candidate ledger matches for a bank statement entry")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<List<BankReconciliationResponses.BankReconciliationCandidateResponse>> candidates(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(bankReconciliationService.candidates(orgId, id));
    }

    @PostMapping("/statements/{id}/reconcile")
    @Operation(summary = "Reconcile a bank statement entry to a ledger entry")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<BankReconciliationResponses.BankStatementEntryResponse> reconcile(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId,
            @RequestBody @Valid BankReconciliationDtos.ReconcileBankStatementRequest request) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(toResponse(bankReconciliationService.reconcile(orgId, id, request)), "Bank statement entry reconciled");
    }

    @PostMapping("/statements/{id}/unreconcile")
    @Operation(summary = "Remove reconciliation from a bank statement entry")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<BankReconciliationResponses.BankStatementEntryResponse> unreconcile(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(toResponse(bankReconciliationService.unreconcile(orgId, id)), "Bank statement entry unreconciled");
    }

    private BankReconciliationResponses.BankStatementEntryResponse toResponse(BankStatementEntry entry) {
        return new BankReconciliationResponses.BankStatementEntryResponse(
                entry.getId(),
                entry.getOrganizationId(),
                entry.getBranchId(),
                entry.getAccountId(),
                entry.getEntryDate(),
                entry.getValueDate(),
                entry.getReferenceNumber(),
                entry.getDescription(),
                entry.getDebitAmount(),
                entry.getCreditAmount(),
                entry.getDebitAmount().subtract(entry.getCreditAmount()),
                entry.getStatus(),
                entry.getMatchedLedgerEntryId(),
                entry.getMatchedOn(),
                entry.getMatchedBy(),
                entry.getRemarks()
        );
    }
}
