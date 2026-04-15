package com.retailmanagement.modules.erp.inventory.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.service.InventoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/inventory-tracking")
@RequiredArgsConstructor
@Tag(name = "ERP Inventory Tracking", description = "ERP batch and serial lookup endpoints")
public class InventoryTrackingController {

    private final InventoryQueryService inventoryQueryService;

    @GetMapping("/batches/product/{productId}")
    @Operation(summary = "List batches for a product")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<List<InventoryDtos.InventoryBatchResponse>> batches(@RequestParam Long organizationId, @PathVariable Long productId) {
        return ErpApiResponse.ok(inventoryQueryService.batchesByProduct(organizationId, productId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/serials/product/{productId}")
    @Operation(summary = "List serial numbers for a product")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<List<InventoryDtos.SerialNumberResponse>> serials(@RequestParam Long organizationId, @PathVariable Long productId) {
        return ErpApiResponse.ok(inventoryQueryService.serialsByProduct(organizationId, productId).stream().map(this::toResponse).toList());
    }

    private InventoryDtos.InventoryBatchResponse toResponse(InventoryBatch batch) {
        return new InventoryDtos.InventoryBatchResponse(batch.getId(), batch.getOrganizationId(), batch.getProductId(),
                batch.getBatchNumber(), batch.getManufacturerBatchNumber(), batch.getManufacturedOn(), batch.getExpiryOn(),
                batch.getBatchType(), batch.getSourceDocumentType(), batch.getSourceDocumentId(), batch.getSourceDocumentLineId(),
                batch.getPurchaseUnitCost(), batch.getSuggestedSalePrice(), batch.getMrp(),
                batch.getStatus(), batch.getCreatedAt(), batch.getUpdatedAt());
    }

    private InventoryDtos.SerialNumberResponse toResponse(SerialNumber serial) {
        return new InventoryDtos.SerialNumberResponse(serial.getId(), serial.getOrganizationId(), serial.getProductId(),
                serial.getBatchId(), serial.getSerialNumber(), serial.getManufacturerSerialNumber(), serial.getStatus(),
                serial.getCurrentWarehouseId(), serial.getCurrentCustomerId(), serial.getWarrantyStartDate(),
                serial.getWarrantyEndDate(), serial.getCreatedAt(), serial.getUpdatedAt());
    }
}
