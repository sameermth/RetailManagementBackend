package com.retailmanagement.modules.erp.catalog.controller;

import com.retailmanagement.modules.erp.catalog.dto.CatalogReferenceDtos;
import com.retailmanagement.modules.erp.catalog.service.CatalogReferenceService;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/catalog")
@RequiredArgsConstructor
@Tag(name = "ERP Catalog", description = "ERP catalog reference endpoints")
public class CatalogReferenceController {

    private final CatalogReferenceService catalogReferenceService;

    @GetMapping("/categories")
    @Operation(summary = "Search categories for product creation")
    @PreAuthorize("hasAuthority('catalog.view')")
    public ErpApiResponse<List<CatalogReferenceDtos.CategoryOptionResponse>> categories(@RequestParam Long organizationId,
                                                                                        @RequestParam(required = false) String query) {
        return ErpApiResponse.ok(catalogReferenceService.searchCategories(organizationId, query).stream()
                .map(category -> new CatalogReferenceDtos.CategoryOptionResponse(
                        category.getId(),
                        category.getName(),
                        category.getParentCategoryId(),
                        category.getIsActive()
                ))
                .toList());
    }

    @GetMapping("/brands")
    @Operation(summary = "Search brands for product creation")
    @PreAuthorize("hasAuthority('catalog.view')")
    public ErpApiResponse<List<CatalogReferenceDtos.BrandOptionResponse>> brands(@RequestParam Long organizationId,
                                                                                 @RequestParam(required = false) String query) {
        return ErpApiResponse.ok(catalogReferenceService.searchBrands(organizationId, query).stream()
                .map(brand -> new CatalogReferenceDtos.BrandOptionResponse(
                        brand.getId(),
                        brand.getName(),
                        brand.getIsActive()
                ))
                .toList());
    }

    @GetMapping("/uoms")
    @Operation(summary = "Search UOMs for product creation")
    @PreAuthorize("hasAuthority('catalog.view')")
    public ErpApiResponse<List<CatalogReferenceDtos.UomOptionResponse>> uoms(@RequestParam(required = false) String query) {
        return ErpApiResponse.ok(catalogReferenceService.searchUoms(query).stream()
                .map(uom -> new CatalogReferenceDtos.UomOptionResponse(
                        uom.getId(),
                        uom.getCode(),
                        uom.getName(),
                        uom.getUomGroupId(),
                        uom.getIsActive()
                ))
                .toList());
    }

    @GetMapping("/tax-groups")
    @Operation(summary = "Search tax groups for product creation")
    @PreAuthorize("hasAuthority('catalog.view')")
    public ErpApiResponse<List<CatalogReferenceDtos.TaxGroupOptionResponse>> taxGroups(@RequestParam Long organizationId,
                                                                                        @RequestParam(required = false) String query) {
        return ErpApiResponse.ok(catalogReferenceService.searchTaxGroups(organizationId, query).stream()
                .map(group -> new CatalogReferenceDtos.TaxGroupOptionResponse(
                        group.getId(),
                        group.getCode(),
                        group.getName(),
                        group.getCgstRate(),
                        group.getSgstRate(),
                        group.getIgstRate(),
                        group.getCessRate(),
                        group.getIsActive()
                ))
                .toList());
    }
}
