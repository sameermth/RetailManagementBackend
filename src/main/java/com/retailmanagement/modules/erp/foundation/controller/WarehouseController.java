package com.retailmanagement.modules.erp.foundation.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.foundation.dto.WarehouseDtos;
import com.retailmanagement.modules.erp.foundation.entity.Warehouse;
import com.retailmanagement.modules.erp.foundation.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/erp/warehouses")
@RequiredArgsConstructor
@Tag(name = "ERP Warehouses", description = "ERP warehouse management endpoints")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    @Operation(summary = "List warehouses for an organization")
    @PreAuthorize("hasAuthority('warehouse.view')")
    public ErpApiResponse<List<WarehouseDtos.WarehouseResponse>> list(
            @RequestParam Long organizationId,
            @RequestParam(required = false) Long branchId
    ) {
        return ErpApiResponse.ok(warehouseService.list(organizationId, branchId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse by id")
    @PreAuthorize("hasAuthority('warehouse.view')")
    public ErpApiResponse<WarehouseDtos.WarehouseResponse> get(@PathVariable Long id, @RequestParam Long organizationId) {
        return ErpApiResponse.ok(toResponse(warehouseService.get(organizationId, id)));
    }

    @PostMapping
    @Operation(summary = "Create warehouse")
    @PreAuthorize("hasAuthority('warehouse.manage')")
    public ErpApiResponse<WarehouseDtos.WarehouseResponse> create(@Valid @RequestBody WarehouseDtos.CreateWarehouseRequest request) {
        return ErpApiResponse.ok(toResponse(warehouseService.create(request)), "ERP warehouse created");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update warehouse")
    @PreAuthorize("hasAuthority('warehouse.manage')")
    public ErpApiResponse<WarehouseDtos.WarehouseResponse> update(
            @PathVariable Long id,
            @RequestParam Long organizationId,
            @Valid @RequestBody WarehouseDtos.UpdateWarehouseRequest request
    ) {
        return ErpApiResponse.ok(toResponse(warehouseService.update(organizationId, id, request)), "ERP warehouse updated");
    }

    private WarehouseDtos.WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseDtos.WarehouseResponse(
                warehouse.getId(),
                warehouse.getOrganizationId(),
                warehouse.getBranchId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getIsPrimary(),
                warehouse.getIsActive(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }
}
