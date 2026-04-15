package com.retailmanagement.modules.erp.inventory.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.service.InventoryBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/inventory-bins")
@RequiredArgsConstructor
@Tag(name = "ERP Inventory Bins", description = "Warehouse bin location master and lookup endpoints")
public class InventoryBinController {

    private final InventoryBinService inventoryBinService;

    @GetMapping
    @Operation(summary = "List warehouse bins")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<java.util.List<InventoryDtos.WarehouseBinLocationResponse>> list(
            @RequestParam Long organizationId,
            @RequestParam Long warehouseId,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        return ErpApiResponse.ok(inventoryBinService.list(organizationId, warehouseId, activeOnly));
    }

    @PostMapping
    @Operation(summary = "Create warehouse bin")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public ErpApiResponse<InventoryDtos.WarehouseBinLocationResponse> create(
            @RequestBody @Valid InventoryDtos.CreateWarehouseBinLocationRequest request
    ) {
        return ErpApiResponse.ok(inventoryBinService.create(request), "Warehouse bin created");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update warehouse bin")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public ErpApiResponse<InventoryDtos.WarehouseBinLocationResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid InventoryDtos.UpdateWarehouseBinLocationRequest request
    ) {
        return ErpApiResponse.ok(inventoryBinService.update(id, request), "Warehouse bin updated");
    }
}
