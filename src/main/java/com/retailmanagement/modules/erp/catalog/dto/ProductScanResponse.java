package com.retailmanagement.modules.erp.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductScanResponse(
        String matchedBy,
        StoreProductSummary storeProduct,
        ProductSummary product,
        SerialMatch serial,
        BatchMatch batch,
        StockSnapshot stock
) {
    public record StoreProductSummary(
            Long id,
            Long productId,
            Long organizationId,
            Long categoryId,
            Long brandId,
            Long baseUomId,
            Long taxGroupId,
            String sku,
            String name,
            String description,
            String inventoryTrackingMode,
            Boolean serialTrackingEnabled,
            Boolean batchTrackingEnabled,
            Boolean expiryTrackingEnabled,
            Boolean fractionalQuantityAllowed,
            Boolean isServiceItem,
            Boolean isActive
    ) {}

    public record ProductSummary(
            Long id,
            String name,
            String description,
            String categoryName,
            String brandName,
            String hsnCode,
            Long baseUomId,
            String inventoryTrackingMode
    ) {}

    public record SerialMatch(
            Long id,
            String serialNumber,
            String manufacturerSerialNumber,
            String status,
            Long currentWarehouseId,
            Long currentCustomerId,
            LocalDate warrantyStartDate,
            LocalDate warrantyEndDate
    ) {}

    public record BatchMatch(
            Long id,
            String batchNumber,
            String manufacturerBatchNumber,
            String status,
            LocalDate manufacturedOn,
            LocalDate expiryOn
    ) {}

    public record StockSnapshot(
            Long warehouseId,
            BigDecimal onHandBaseQuantity,
            BigDecimal reservedBaseQuantity,
            BigDecimal availableBaseQuantity
    ) {}
}
