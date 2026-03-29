package com.retailmanagement.modules.erp.inventory.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.entity.StockAdjustment;
import com.retailmanagement.modules.erp.inventory.entity.StockTransfer;
import com.retailmanagement.modules.erp.inventory.service.InventoryOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/inventory-operations")
@RequiredArgsConstructor
@Validated
@Tag(name = "ERP Inventory Operations", description = "ERP manual adjustment and stock transfer endpoints")
public class InventoryOperationsController {

    private final InventoryOperationsService inventoryOperationsService;

    @PostMapping("/adjustments/manual")
    @Operation(summary = "Post manual stock adjustment")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public ErpApiResponse<InventoryDtos.StockAdjustmentResponse> manualAdjustment(@RequestBody @Valid ManualAdjustmentRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);

        StockAdjustment adjustment = inventoryOperationsService.createManualAdjustment(
                orgId,
                branchId,
                request.warehouseId(),
                request.productId(),
                request.uomId(),
                request.quantityDelta(),
                request.baseQuantityDelta(),
                request.unitCost(),
                request.reason()
        );
        return ErpApiResponse.ok(toResponse(adjustment), "Manual adjustment posted");
    }

    @PostMapping("/transfers")
    @Operation(summary = "Create stock transfer")
    @PreAuthorize("hasAuthority('inventory.transfer')")
    public ErpApiResponse<InventoryDtos.StockTransferResponse> transfer(@RequestBody @Valid StockTransferRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);

        StockTransfer transfer = inventoryOperationsService.createTransfer(
                orgId,
                branchId,
                request.fromWarehouseId(),
                request.toWarehouseId(),
                request.lines().stream()
                        .map(line -> new InventoryOperationsService.TransferLineCommand(
                                line.productId(),
                                line.uomId(),
                                line.quantity(),
                                line.baseQuantity()
                        ))
                        .toList()
        );
        return ErpApiResponse.ok(toResponse(transfer), "Stock transfer posted");
    }

    public record ManualAdjustmentRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long warehouseId,
            @NotNull Long productId,
            @NotNull Long uomId,
            @NotNull BigDecimal quantityDelta,
            @NotNull BigDecimal baseQuantityDelta,
            BigDecimal unitCost,
            @NotNull String reason
    ) {}

    public record StockTransferRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long fromWarehouseId,
            @NotNull Long toWarehouseId,
            @NotEmpty List<StockTransferLineRequest> lines
    ) {}

    public record StockTransferLineRequest(
            @NotNull Long productId,
            @NotNull Long uomId,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal baseQuantity
    ) {}

    private InventoryDtos.StockAdjustmentResponse toResponse(StockAdjustment adjustment) {
        return new InventoryDtos.StockAdjustmentResponse(adjustment.getId(), adjustment.getOrganizationId(), adjustment.getBranchId(),
                adjustment.getWarehouseId(), adjustment.getAdjustmentNumber(), adjustment.getAdjustmentDate(), adjustment.getReason(),
                adjustment.getStatus(), adjustment.getCreatedAt(), adjustment.getUpdatedAt());
    }

    private InventoryDtos.StockTransferResponse toResponse(StockTransfer transfer) {
        return new InventoryDtos.StockTransferResponse(transfer.getId(), transfer.getOrganizationId(), transfer.getBranchId(),
                transfer.getFromWarehouseId(), transfer.getToWarehouseId(), transfer.getTransferNumber(), transfer.getTransferDate(),
                transfer.getStatus(), transfer.getCreatedAt(), transfer.getUpdatedAt());
    }
}
