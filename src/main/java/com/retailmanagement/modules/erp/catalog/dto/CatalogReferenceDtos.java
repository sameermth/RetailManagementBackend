package com.retailmanagement.modules.erp.catalog.dto;

import java.math.BigDecimal;

public final class CatalogReferenceDtos {
    private CatalogReferenceDtos() {}

    public record CategoryOptionResponse(
            Long id,
            String name,
            Long parentCategoryId,
            Boolean isActive
    ) {}

    public record BrandOptionResponse(
            Long id,
            String name,
            Boolean isActive
    ) {}

    public record UomOptionResponse(
            Long id,
            String code,
            String name,
            Long uomGroupId,
            Boolean isActive
    ) {}

    public record TaxGroupOptionResponse(
            Long id,
            String code,
            String name,
            BigDecimal cgstRate,
            BigDecimal sgstRate,
            BigDecimal igstRate,
            BigDecimal cessRate,
            Boolean isActive
    ) {}
}
