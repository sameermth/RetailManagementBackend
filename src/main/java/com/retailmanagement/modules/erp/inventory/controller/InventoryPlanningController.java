package com.retailmanagement.modules.erp.inventory.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.service.InventoryPlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/inventory-planning")
@RequiredArgsConstructor
@Tag(name = "ERP Inventory Planning", description = "ERP replenishment and stock rebalancing recommendation endpoints")
public class InventoryPlanningController {

    private final InventoryPlanningService inventoryPlanningService;

    @GetMapping("/replenishment")
    @Operation(summary = "List replenishment recommendations")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<java.util.List<InventoryDtos.InventoryReplenishmentRecommendationResponse>> replenishment(
            @RequestParam Long organizationId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "true") boolean actionableOnly
    ) {
        return ErpApiResponse.ok(inventoryPlanningService.listRecommendations(organizationId, branchId, warehouseId, actionableOnly));
    }

    @PostMapping("/replenishment/draft-purchase-orders")
    @Operation(summary = "Create draft purchase order from replenishment recommendation")
    @PreAuthorize("hasAuthority('purchase.create') or hasAuthority('purchases.create')")
    public ErpApiResponse<InventoryDtos.DraftPurchaseOrderSummaryResponse> createDraftPurchaseOrder(
            @Valid @RequestBody InventoryDtos.CreateReplenishmentPurchaseOrderRequest request
    ) {
        return ErpApiResponse.ok(
                inventoryPlanningService.createDraftPurchaseOrder(request),
                "Draft purchase order created from inventory planning"
        );
    }

    @PostMapping("/replenishment/transfers")
    @Operation(summary = "Create stock transfer from replenishment recommendation")
    @PreAuthorize("hasAuthority('inventory.transfer')")
    public ErpApiResponse<InventoryDtos.ReplenishmentTransferSummaryResponse> createTransfer(
            @Valid @RequestBody InventoryDtos.CreateReplenishmentTransferRequest request
    ) {
        return ErpApiResponse.ok(
                inventoryPlanningService.createRecommendedTransfer(request),
                "Stock transfer created from inventory planning"
        );
    }
}
