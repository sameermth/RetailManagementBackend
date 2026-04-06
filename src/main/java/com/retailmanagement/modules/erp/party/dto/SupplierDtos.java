package com.retailmanagement.modules.erp.party.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SupplierDtos {
    private SupplierDtos() {}

    public record UpsertSupplierRequest(
            String supplierCode,
            @NotBlank String name,
            String legalName,
            String tradeName,
            String phone,
            String email,
            String gstin,
            Long linkedOrganizationId,
            String billingAddress,
            String shippingAddress,
            String state,
            String stateCode,
            String contactPersonName,
            String contactPersonPhone,
            String contactPersonEmail,
            String paymentTerms,
            Boolean isPlatformLinked,
            String notes,
            String status
    ) {}

    public record SupplierResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long linkedOrganizationId,
            String supplierCode,
            String name,
            String legalName,
            String tradeName,
            String phone,
            String email,
            String gstin,
            String billingAddress,
            String shippingAddress,
            String state,
            String stateCode,
            String contactPersonName,
            String contactPersonPhone,
            String contactPersonEmail,
            String paymentTerms,
            Boolean isPlatformLinked,
            String notes,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record UpsertSupplierProductRequest(
            @NotNull Long productId,
            String supplierProductCode,
            String supplierProductName,
            Integer priority,
            Boolean isPreferred,
            Boolean isActive
    ) {}

    public record SupplierProductResponse(
            Long id,
            Long organizationId,
            Long supplierId,
            Long productId,
            String supplierProductCode,
            String supplierProductName,
            Integer priority,
            Boolean isPreferred,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record UpsertStoreSupplierTermsRequest(
            String paymentTerms,
            BigDecimal creditLimit,
            Integer creditDays,
            Boolean isPreferred,
            Boolean isActive,
            LocalDate contractStart,
            LocalDate contractEnd,
            Boolean orderViaEmail,
            Boolean orderViaWhatsapp,
            String remarks
    ) {}

    public record StoreSupplierTermsResponse(
            Long id,
            Long organizationId,
            Long supplierId,
            String paymentTerms,
            BigDecimal creditLimit,
            Integer creditDays,
            Boolean isPreferred,
            Boolean isActive,
            LocalDate contractStart,
            LocalDate contractEnd,
            Boolean orderViaEmail,
            Boolean orderViaWhatsapp,
            String remarks,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record UpsertStoreProductSupplierPreferenceRequest(
            @NotNull Long supplierId,
            @NotNull Long supplierProductId,
            Boolean isActive,
            String remarks
    ) {}

    public record StoreProductSupplierPreferenceResponse(
            Long id,
            Long organizationId,
            Long storeProductId,
            Long supplierId,
            Long supplierProductId,
            Boolean isActive,
            String remarks,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record StoreProductSupplierLinkResponse(
            Long supplierId,
            String supplierCode,
            String supplierName,
            Long supplierProductId,
            String supplierProductCode,
            String supplierProductName,
            Integer priority,
            Boolean supplierPreferred,
            Boolean storeProductPreferred,
            Boolean isActive
    ) {}

    public record StoreProductSuppliersResponse(
            Long storeProductId,
            Long productId,
            Long preferredSupplierId,
            Long preferredSupplierProductId,
            List<StoreProductSupplierLinkResponse> supplierLinks
    ) {}

    public record UpsertStoreProductSupplierLinkRequest(
            @NotNull Long supplierId,
            Long supplierProductId,
            String supplierProductCode,
            String supplierProductName,
            Integer priority,
            Boolean isPreferred,
            Boolean isActive
    ) {}

    public record UpsertStoreProductSuppliersRequest(
            @NotNull @jakarta.validation.Valid List<UpsertStoreProductSupplierLinkRequest> supplierLinks,
            Long preferredSupplierId,
            Long preferredSupplierProductId,
            Boolean preferredIsActive,
            String preferredRemarks
    ) {}

    public record PurchasableStoreProductResponse(
            Long storeProductId,
            Long productId,
            Long supplierProductId,
            String sku,
            String name,
            String supplierProductCode,
            String supplierProductName,
            Boolean supplierPreferred,
            Integer supplierPriority
    ) {}

    public record SupplierCatalogResponse(
            SupplierResponse supplier,
            StoreSupplierTermsResponse terms,
            List<PurchasableStoreProductResponse> products
    ) {}
}
