package com.retailmanagement.modules.erp.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ProductDtos {
    private ProductDtos() {}

    public record CreateStoreProductRequest(
            @NotNull Long organizationId,
            Long productId,
            @NotNull Long categoryId,
            @NotNull Long brandId,
            @NotNull Long baseUomId,
            @NotNull Long taxGroupId,
            @NotBlank String sku,
            @NotBlank String name,
            String description,
            String hsnCode,
            @NotBlank String inventoryTrackingMode,
            Boolean serialTrackingEnabled,
            Boolean batchTrackingEnabled,
            Boolean expiryTrackingEnabled,
            Boolean fractionalQuantityAllowed,
            BigDecimal minStockBaseQty,
            BigDecimal reorderLevelBaseQty,
            BigDecimal defaultSalePrice,
            Boolean isServiceItem,
            Boolean isActive
    ) {}

    public record LinkCatalogProductRequest(
            @NotNull Long organizationId,
            @NotNull Long productId,
            @NotNull Long categoryId,
            @NotNull Long brandId,
            @NotNull Long taxGroupId,
            String sku,
            String name,
            String description,
            BigDecimal minStockBaseQty,
            BigDecimal reorderLevelBaseQty,
            BigDecimal defaultSalePrice,
            Boolean isActive
    ) {}

    public record StoreProductResponse(
            Long id,
            Long organizationId,
            Long productId,
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
            BigDecimal minStockBaseQty,
            BigDecimal reorderLevelBaseQty,
            BigDecimal defaultSalePrice,
            Boolean isServiceItem,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record UpsertStoreProductPriceRequest(
            String priceType,
            String customerSegment,
            @NotNull BigDecimal price,
            BigDecimal minQuantity,
            java.time.LocalDate effectiveFrom,
            java.time.LocalDate effectiveTo,
            Boolean isDefault,
            Boolean isActive
    ) {}

    public record StoreProductPriceResponse(
            Long id,
            Long organizationId,
            Long storeProductId,
            String priceType,
            String customerSegment,
            BigDecimal price,
            BigDecimal minQuantity,
            java.time.LocalDate effectiveFrom,
            java.time.LocalDate effectiveTo,
            Boolean isDefault,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ProductResponse(
            Long id,
            String name,
            String description,
            String categoryName,
            String brandName,
            String hsnCode,
            Long baseUomId,
            String inventoryTrackingMode,
            Boolean serialTrackingEnabled,
            Boolean batchTrackingEnabled,
            Boolean expiryTrackingEnabled,
            Boolean fractionalQuantityAllowed,
            Boolean isServiceItem,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record DiscoverableProductResponse(
            Long id,
            String name,
            String description,
            String categoryName,
            String brandName,
            String hsnCode,
            Long baseUomId,
            String inventoryTrackingMode,
            Boolean serialTrackingEnabled,
            Boolean batchTrackingEnabled,
            Boolean expiryTrackingEnabled,
            Boolean fractionalQuantityAllowed,
            Boolean isServiceItem,
            Boolean isActive
    ) {}
}
