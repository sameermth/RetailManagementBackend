package com.retailmanagement.modules.erp.finance.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.finance.dto.ErpFinanceDtos;
import com.retailmanagement.modules.erp.finance.dto.RecurringFinanceDtos;
import com.retailmanagement.modules.erp.finance.dto.RecurringFinanceResponses;
import com.retailmanagement.modules.erp.finance.entity.RecurringJournal;
import com.retailmanagement.modules.erp.finance.service.RecurringJournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/finance/recurring-journals")
@RequiredArgsConstructor
@Tag(name = "ERP Finance", description = "Recurring journal endpoints")
public class RecurringJournalController {

    private final RecurringJournalService recurringJournalService;

    @GetMapping
    @Operation(summary = "List recurring journal templates")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<List<RecurringFinanceResponses.RecurringJournalSummaryResponse>> list(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(recurringJournalService.list(orgId).stream().map(this::toSummary).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recurring journal template")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<RecurringFinanceResponses.RecurringJournalResponse> get(@PathVariable Long id,
                                                                                  @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(recurringJournalService.get(orgId, id));
    }

    @PostMapping
    @Operation(summary = "Create recurring journal template")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<RecurringFinanceResponses.RecurringJournalResponse> create(@RequestBody @Valid RecurringFinanceDtos.CreateRecurringJournalRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(recurringJournalService.create(orgId, branchId, request), "Recurring journal template created");
    }

    @PostMapping("/{id}/run")
    @Operation(summary = "Run recurring journal template now")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<ErpFinanceDtos.VoucherResponse> run(@PathVariable Long id,
                                                              @RequestParam(required = false) Long organizationId,
                                                              @RequestBody(required = false) RecurringFinanceDtos.RunRecurringJournalRequest request) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(recurringJournalService.run(orgId, id, request == null ? null : request.runDate()), "Recurring journal voucher generated");
    }

    private RecurringFinanceResponses.RecurringJournalSummaryResponse toSummary(RecurringJournal journal) {
        return new RecurringFinanceResponses.RecurringJournalSummaryResponse(
                journal.getId(),
                journal.getOrganizationId(),
                journal.getBranchId(),
                journal.getTemplateNumber(),
                journal.getVoucherType(),
                journal.getFrequency(),
                journal.getStartDate(),
                journal.getNextRunDate(),
                journal.getEndDate(),
                journal.getIsActive(),
                journal.getLastVoucherId()
        );
    }
}
