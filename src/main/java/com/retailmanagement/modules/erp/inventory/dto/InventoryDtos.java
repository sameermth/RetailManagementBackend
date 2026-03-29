package com.retailmanagement.modules.erp.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class InventoryDtos {
    private InventoryDtos() {}

    public record InventoryBalanceResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
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
            LocalDateTime createdAt,
            LocalDateTime updatedAt
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
}
