package com.retailmanagement.modules.erp.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class InventoryDtos {
    private InventoryDtos() {}

    public record InventoryBalanceResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long binLocationId,
            Long productId,
            Long batchId,
            BigDecimal onHandBaseQuantity,
            BigDecimal reservedBaseQuantity,
            BigDecimal availableBaseQuantity,
            BigDecimal avgCost,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record StockMovementResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long binLocationId,
            Long productId,
            String movementType,
            String referenceType,
            Long referenceId,
            String referenceNumber,
            String direction,
            Long uomId,
            BigDecimal quantity,
            BigDecimal baseQuantity,
            BigDecimal unitCost,
            BigDecimal totalCost,
            LocalDateTime movementAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record InventoryBatchResponse(
            Long id,
            Long organizationId,
            Long productId,
            String batchNumber,
            String manufacturerBatchNumber,
            LocalDate manufacturedOn,
            LocalDate expiryOn,
            String batchType,
            String sourceDocumentType,
            Long sourceDocumentId,
            Long sourceDocumentLineId,
            BigDecimal purchaseUnitCost,
            BigDecimal suggestedSalePrice,
            BigDecimal mrp,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record SerialNumberResponse(
            Long id,
            Long organizationId,
            Long productId,
            Long batchId,
            String serialNumber,
            String manufacturerSerialNumber,
            String status,
            Long currentWarehouseId,
            Long currentCustomerId,
            LocalDate warrantyStartDate,
            LocalDate warrantyEndDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record StockAdjustmentResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            String adjustmentNumber,
            LocalDate adjustmentDate,
            String reason,
            String status,
            Long lineCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record StockTransferResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long fromWarehouseId,
            Long toWarehouseId,
            String transferNumber,
            LocalDate transferDate,
            String status,
            Long lineCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record WarehouseBinLocationResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            String code,
            String name,
            String zoneCode,
            Integer sortOrder,
            Boolean isDefault,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record CreateWarehouseBinLocationRequest(
            @NotNull Long organizationId,
            @NotNull Long warehouseId,
            @NotNull String code,
            @NotNull String name,
            String zoneCode,
            Integer sortOrder,
            Boolean isDefault,
            Boolean isActive
    ) {}

    public record UpdateWarehouseBinLocationRequest(
            @NotNull String code,
            @NotNull String name,
            String zoneCode,
            Integer sortOrder,
            Boolean isDefault,
            Boolean isActive
    ) {}

    public record InventoryReservationResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long productId,
            Long batchId,
            Long serialNumberId,
            String sourceDocumentType,
            Long sourceDocumentId,
            Long sourceDocumentLineId,
            BigDecimal reservedBaseQuantity,
            LocalDateTime expiresAt,
            LocalDateTime releasedAt,
            String releaseReason,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record CreateStockCountSessionRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long warehouseId,
            String notes
    ) {}

    public record UpsertStockCountLinesRequest(
            @NotEmpty java.util.List<@Valid StockCountLineRequest> lines
    ) {}

    public record StockCountLineRequest(
            @NotNull Long productId,
            Long batchId,
            @NotNull BigDecimal countedBaseQuantity,
            String remarks
    ) {}

    public record StockCountSessionResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            String countNumber,
            LocalDate countDate,
            String status,
            String notes,
            LocalDateTime startedAt,
            LocalDateTime submittedAt,
            LocalDateTime variancePostedAt,
            long totalLines,
            long varianceLines,
            java.util.List<StockCountLineResponse> lines,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record StockCountLineResponse(
            Long id,
            Long stockCountSessionId,
            Long productId,
            Long batchId,
            BigDecimal expectedBaseQuantity,
            BigDecimal countedBaseQuantity,
            BigDecimal varianceBaseQuantity,
            Long postedAdjustmentId,
            String remarks,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record InventoryReplenishmentRecommendationResponse(
            Long storeProductId,
            Long productMasterId,
            String sku,
            String name,
            Long branchId,
            Long warehouseId,
            String warehouseCode,
            String warehouseName,
            BigDecimal availableBaseQuantity,
            BigDecimal minStockBaseQty,
            BigDecimal reorderLevelBaseQty,
            BigDecimal recommendedReplenishmentBaseQty,
            InternalTransferSuggestion transferSuggestion,
            PurchaseSuggestion purchaseSuggestion,
            Boolean actionRequired
    ) {}

    public record InternalTransferSuggestion(
            Long sourceWarehouseId,
            String sourceWarehouseCode,
            String sourceWarehouseName,
            BigDecimal sourceAvailableBaseQty,
            BigDecimal sourceExcessBaseQty,
            BigDecimal recommendedTransferBaseQty
    ) {}

    public record PurchaseSuggestion(
            Long supplierId,
            String supplierCode,
            String supplierName,
            Long supplierProductId,
            BigDecimal recommendedPurchaseBaseQty,
            Boolean openPurchaseOrderExists
    ) {}

    public record CreateReplenishmentPurchaseOrderRequest(
            @NotNull Long organizationId,
            Long branchId,
            Long warehouseId,
            @NotNull Long storeProductId,
            Long supplierId,
            BigDecimal quantity,
            String remarks
    ) {}

    public record CreateReplenishmentTransferRequest(
            @NotNull Long organizationId,
            Long branchId,
            @NotNull Long storeProductId,
            @NotNull Long sourceWarehouseId,
            @NotNull Long targetWarehouseId,
            BigDecimal quantity,
            String remarks
    ) {}

    public record DraftPurchaseOrderSummaryResponse(
            Long purchaseOrderId,
            String purchaseOrderNumber,
            String status,
            Long supplierId,
            String supplierName,
            BigDecimal quantity,
            LocalDate createdOn
    ) {}

    public record ReplenishmentTransferSummaryResponse(
            Long stockTransferId,
            String transferNumber,
            String status,
            Long sourceWarehouseId,
            Long targetWarehouseId,
            BigDecimal quantity,
            LocalDate createdOn
    ) {}
}
