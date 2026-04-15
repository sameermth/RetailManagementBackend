package com.retailmanagement.modules.erp.inventory.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.entity.StockMovement;
import com.retailmanagement.modules.erp.inventory.service.InventoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController("erpStockMovementController")
@RequestMapping("/api/erp/stock-movements")
@RequiredArgsConstructor
@Tag(name = "ERP Stock Movements", description = "ERP stock movement query endpoints")
public class StockMovementController {

    private final InventoryQueryService inventoryQueryService;

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "List stock movements by warehouse")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<List<InventoryDtos.StockMovementResponse>> byWarehouse(@RequestParam Long organizationId, @PathVariable Long warehouseId) {
        return ErpApiResponse.ok(inventoryQueryService.movementsByWarehouse(organizationId, warehouseId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "List stock movements by product")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<List<InventoryDtos.StockMovementResponse>> byProduct(@RequestParam Long organizationId, @PathVariable Long productId) {
        return ErpApiResponse.ok(inventoryQueryService.movementsByProduct(organizationId, productId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/bin/{binLocationId}")
    @Operation(summary = "List stock movements by bin")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<List<InventoryDtos.StockMovementResponse>> byBin(@RequestParam Long organizationId, @PathVariable Long binLocationId) {
        return ErpApiResponse.ok(inventoryQueryService.movementsByBin(organizationId, binLocationId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/reference/{referenceType}/{referenceId}")
    @Operation(summary = "List stock movements by reference")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<List<InventoryDtos.StockMovementResponse>> byReference(
            @RequestParam Long organizationId,
            @PathVariable String referenceType,
            @PathVariable Long referenceId
    ) {
        return ErpApiResponse.ok(inventoryQueryService.movementsByReference(organizationId, referenceType, referenceId).stream().map(this::toResponse).toList());
    }

    private InventoryDtos.StockMovementResponse toResponse(StockMovement movement) {
        return new InventoryDtos.StockMovementResponse(movement.getId(), movement.getOrganizationId(), movement.getBranchId(),
                movement.getWarehouseId(), movement.getBinLocationId(), movement.getProductId(), movement.getMovementType(), movement.getReferenceType(),
                movement.getReferenceId(), movement.getReferenceNumber(), movement.getDirection(), movement.getUomId(),
                movement.getQuantity(), movement.getBaseQuantity(), movement.getUnitCost(), movement.getTotalCost(),
                movement.getMovementAt(), movement.getCreatedAt(), movement.getUpdatedAt());
    }
}
