package com.retailmanagement.modules.erp.sales.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.sales.dto.RecurringSalesDtos;
import com.retailmanagement.modules.erp.sales.dto.RecurringSalesResponses;
import com.retailmanagement.modules.erp.sales.entity.RecurringSalesInvoice;
import com.retailmanagement.modules.erp.sales.service.RecurringSalesInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/sales/recurring-invoices")
@RequiredArgsConstructor
@Tag(name = "ERP Sales", description = "Recurring sales invoice endpoints")
public class RecurringSalesInvoiceController {

    private final RecurringSalesInvoiceService recurringSalesInvoiceService;

    @GetMapping
    @Operation(summary = "List recurring sales invoice templates")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<RecurringSalesResponses.RecurringSalesInvoiceSummaryResponse>> list(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(recurringSalesInvoiceService.list(orgId).stream().map(this::toSummary).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recurring sales invoice template")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<RecurringSalesResponses.RecurringSalesInvoiceResponse> get(@PathVariable Long id,
                                                                                     @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(recurringSalesInvoiceService.get(orgId, id));
    }

    @PostMapping
    @Operation(summary = "Create recurring sales invoice template")
    @PreAuthorize("hasAnyAuthority('sales.create','sales.post')")
    public ErpApiResponse<RecurringSalesResponses.RecurringSalesInvoiceResponse> create(@RequestBody @Valid RecurringSalesDtos.CreateRecurringSalesInvoiceRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(recurringSalesInvoiceService.create(orgId, branchId, request), "Recurring sales invoice template created");
    }

    @PostMapping("/{id}/run")
    @Operation(summary = "Run recurring sales invoice template now")
    @PreAuthorize("hasAnyAuthority('sales.create','sales.post')")
    public ErpApiResponse<com.retailmanagement.modules.erp.sales.dto.ErpSalesResponses.SalesInvoiceResponse> run(@PathVariable Long id,
                                                                                                                   @RequestParam(required = false) Long organizationId,
                                                                                                                   @RequestBody(required = false) RecurringSalesDtos.RunRecurringSalesInvoiceRequest request) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(recurringSalesInvoiceService.run(orgId, id, request == null ? null : request.runDate()), "Recurring sales invoice generated");
    }

    private RecurringSalesResponses.RecurringSalesInvoiceSummaryResponse toSummary(RecurringSalesInvoice template) {
        return new RecurringSalesResponses.RecurringSalesInvoiceSummaryResponse(
                template.getId(),
                template.getOrganizationId(),
                template.getBranchId(),
                template.getWarehouseId(),
                template.getCustomerId(),
                template.getTemplateNumber(),
                template.getFrequency(),
                template.getStartDate(),
                template.getNextRunDate(),
                template.getEndDate(),
                template.getIsActive(),
                template.getLastSalesInvoiceId()
        );
    }
}
