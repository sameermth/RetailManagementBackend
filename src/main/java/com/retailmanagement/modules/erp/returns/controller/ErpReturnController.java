package com.retailmanagement.modules.erp.returns.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.returns.dto.ErpReturnDtos;
import com.retailmanagement.modules.erp.returns.dto.ErpReturnResponses;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturn;
import com.retailmanagement.modules.erp.returns.entity.SalesReturn;
import com.retailmanagement.modules.erp.returns.service.ErpReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/returns")
@RequiredArgsConstructor
@Tag(name = "ERP Returns", description = "ERP sales and purchase return endpoints")
public class ErpReturnController {

    private final ErpReturnService erpReturnService;

    @GetMapping("/sales")
    @Operation(summary = "List sales returns")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<ErpReturnResponses.SalesReturnSummaryResponse>> listSalesReturns(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpReturnService.listSalesReturns(orgId).stream().map(this::toSalesReturnSummary).toList());
    }

    @GetMapping("/sales/{id}")
    @Operation(summary = "Get sales return by id")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<ErpReturnResponses.SalesReturnResponse> getSalesReturn(@PathVariable Long id) {
        return ErpApiResponse.ok(erpReturnService.getSalesReturn(id));
    }

    @PostMapping("/sales")
    @Operation(summary = "Create sales return")
    @PreAuthorize("hasAuthority('sales.return')")
    public ErpApiResponse<ErpReturnResponses.SalesReturnResponse> createSalesReturn(@RequestBody @Valid ErpReturnDtos.CreateSalesReturnRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpReturnService.createSalesReturn(orgId, branchId, request), "Sales return created");
    }

    @PostMapping("/sales/{id}/inspect")
    @Operation(summary = "Inspect and post sales return")
    @PreAuthorize("hasAuthority('sales.return')")
    public ErpApiResponse<ErpReturnResponses.SalesReturnResponse> inspectSalesReturn(@PathVariable Long id, @RequestBody @Valid ErpReturnDtos.InspectSalesReturnRequest request) {
        return ErpApiResponse.ok(erpReturnService.inspectSalesReturn(id, request), "Sales return inspected");
    }

    @GetMapping("/purchases")
    @Operation(summary = "List purchase returns")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<List<ErpReturnResponses.PurchaseReturnSummaryResponse>> listPurchaseReturns(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpReturnService.listPurchaseReturns(orgId).stream().map(this::toPurchaseReturnSummary).toList());
    }

    @GetMapping("/purchases/{id}")
    @Operation(summary = "Get purchase return by id")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<ErpReturnResponses.PurchaseReturnResponse> getPurchaseReturn(@PathVariable Long id) {
        return ErpApiResponse.ok(erpReturnService.getPurchaseReturn(id));
    }

    @PostMapping("/purchases")
    @Operation(summary = "Create purchase return")
    @PreAuthorize("hasAuthority('purchase.return')")
    public ErpApiResponse<ErpReturnResponses.PurchaseReturnResponse> createPurchaseReturn(@RequestBody @Valid ErpReturnDtos.CreatePurchaseReturnRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpReturnService.createPurchaseReturn(orgId, branchId, request), "Purchase return created");
    }

    private ErpReturnResponses.SalesReturnSummaryResponse toSalesReturnSummary(SalesReturn item) {
        return new ErpReturnResponses.SalesReturnSummaryResponse(
                item.getId(), item.getOrganizationId(), item.getBranchId(), item.getWarehouseId(),
                item.getCustomerId(), item.getOriginalSalesInvoiceId(), item.getReturnNumber(), item.getReturnDate(),
                item.getSubtotal(), item.getTaxAmount(), item.getTotalAmount(), item.getStatus()
        );
    }

    private ErpReturnResponses.PurchaseReturnSummaryResponse toPurchaseReturnSummary(PurchaseReturn item) {
        return new ErpReturnResponses.PurchaseReturnSummaryResponse(
                item.getId(), item.getOrganizationId(), item.getBranchId(), item.getWarehouseId(),
                item.getSupplierId(), item.getOriginalPurchaseReceiptId(), item.getReturnNumber(), item.getReturnDate(),
                item.getSubtotal(), item.getTaxAmount(), item.getTotalAmount(), item.getStatus()
        );
    }
}
