package com.retailmanagement.modules.erp.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ProductDtos {
    private ProductDtos() {}

    public record CreateStoreProductRequest(
            @NotNull Long organizationId,
            Long productId,
            @NotNull Long categoryId,
            @NotNull Long brandId,
            @NotNull Long baseUomId,
            Long taxGroupId,
            @NotBlank String sku,
            @NotBlank String name,
            String description,
            String hsnCode,
            @NotBlank String inventoryTrackingMode,
            Boolean serialTrackingEnabled,
            Boolean batchTrackingEnabled,
            Boolean expiryTrackingEnabled,
            Boolean fractionalQuantityAllowed,
            List<ProductAttributeDtos.UpsertProductAttributeValueRequest> attributes,
            BigDecimal minStockBaseQty,
            BigDecimal reorderLevelBaseQty,
            BigDecimal defaultSalePrice,
            BigDecimal defaultMrp,
            Integer defaultWarrantyMonths,
            String warrantyTerms,
            Boolean isBundle,
            String bundlePricingMode,
            Boolean isServiceItem,
            Boolean isActive
    ) {}

    public record LinkCatalogProductRequest(
            @NotNull Long organizationId,
            @NotNull Long productId,
            @NotNull Long categoryId,
            @NotNull Long brandId,
            Long taxGroupId,
            String sku,
            String name,
            String description,
            List<ProductAttributeDtos.UpsertProductAttributeValueRequest> attributes,
            BigDecimal minStockBaseQty,
            BigDecimal reorderLevelBaseQty,
            BigDecimal defaultSalePrice,
            BigDecimal defaultMrp,
            Integer defaultWarrantyMonths,
            String warrantyTerms,
            Boolean isBundle,
            String bundlePricingMode,
            Boolean isActive
    ) {}

    public record StoreProductResponse(
            Long id,
            Long organizationId,
            Long productId,
            Long categoryId,
            String categoryName,
            Long brandId,
            String brandName,
            Long baseUomId,
            String baseUomCode,
            String baseUomName,
            Long taxGroupId,
            String taxGroupCode,
            String taxGroupName,
            List<ProductAttributeDtos.ProductAttributeValueResponse> attributes,
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
            BigDecimal defaultMrp,
            Integer defaultWarrantyMonths,
            String warrantyTerms,
            Boolean isBundle,
            String bundlePricingMode,
            List<StoreProductBundleComponentResponse> bundleComponents,
            Boolean isServiceItem,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record UpsertStoreProductBundleRequest(
            @NotNull Long organizationId,
            @NotEmpty List<StoreProductBundleComponentRequest> components
    ) {}

    public record StoreProductBundleComponentRequest(
            @NotNull Long componentStoreProductId,
            @NotNull BigDecimal componentQuantity,
            @NotNull BigDecimal componentBaseQuantity,
            Integer sortOrder
    ) {}

    public record StoreProductBundleComponentResponse(
            Long id,
            Long componentStoreProductId,
            String componentSku,
            String componentName,
            BigDecimal componentQuantity,
            BigDecimal componentBaseQuantity,
            Integer sortOrder
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

    public record TaxGroupSuggestionResponse(
            String hsnCode,
            Long taxGroupId,
            String taxGroupCode,
            String taxGroupName,
            BigDecimal cgstRate,
            BigDecimal sgstRate,
            BigDecimal igstRate,
            BigDecimal cessRate,
            java.time.LocalDate effectiveFrom,
            Boolean matched,
            String message
    ) {}
}
