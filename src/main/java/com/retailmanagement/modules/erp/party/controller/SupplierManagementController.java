package com.retailmanagement.modules.erp.party.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.party.dto.SupplierDtos;
import com.retailmanagement.modules.erp.party.service.SupplierManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/suppliers")
@RequiredArgsConstructor
@Tag(name = "ERP Suppliers", description = "ERP supplier relationship and sourcing endpoints")
public class SupplierManagementController {

    private final SupplierManagementService supplierManagementService;

    @GetMapping
    @Operation(summary = "List suppliers")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<List<SupplierDtos.SupplierResponse>> listSuppliers(@RequestParam Long organizationId) {
        return ErpApiResponse.ok(supplierManagementService.listSuppliers(organizationId));
    }

    @PostMapping
    @Operation(summary = "Create supplier")
    @PreAuthorize("hasAuthority('purchase.create')")
    public ErpApiResponse<SupplierDtos.SupplierResponse> createSupplier(
            @RequestParam Long organizationId,
            @RequestParam Long branchId,
            @Valid @RequestBody SupplierDtos.UpsertSupplierRequest request
    ) {
        return ErpApiResponse.ok(supplierManagementService.createSupplier(organizationId, branchId, request), "Supplier created");
    }

    @PutMapping("/{supplierId}")
    @Operation(summary = "Update supplier")
    @PreAuthorize("hasAuthority('purchase.create')")
    public ErpApiResponse<SupplierDtos.SupplierResponse> updateSupplier(
            @RequestParam Long organizationId,
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierDtos.UpsertSupplierRequest request
    ) {
        return ErpApiResponse.ok(supplierManagementService.updateSupplier(organizationId, supplierId, request), "Supplier updated");
    }

    @GetMapping("/{supplierId}/catalog")
    @Operation(summary = "Get supplier purchasable catalog for the organization")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<SupplierDtos.SupplierCatalogResponse> supplierCatalog(
            @RequestParam Long organizationId,
            @PathVariable Long supplierId
    ) {
        return ErpApiResponse.ok(supplierManagementService.supplierCatalog(organizationId, supplierId));
    }

    @GetMapping("/{supplierId}/products")
    @Operation(summary = "List supplier product links")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<List<SupplierDtos.SupplierProductResponse>> listSupplierProducts(
            @RequestParam Long organizationId,
            @PathVariable Long supplierId
    ) {
        return ErpApiResponse.ok(supplierManagementService.listSupplierProducts(organizationId, supplierId));
    }

    @PostMapping("/{supplierId}/products")
    @Operation(summary = "Create supplier product link")
    @PreAuthorize("hasAuthority('purchase.create')")
    public ErpApiResponse<SupplierDtos.SupplierProductResponse> createSupplierProduct(
            @RequestParam Long organizationId,
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierDtos.UpsertSupplierProductRequest request
    ) {
        return ErpApiResponse.ok(
                supplierManagementService.upsertSupplierProduct(organizationId, supplierId, null, request),
                "Supplier product linked"
        );
    }

    @PutMapping("/{supplierId}/products/{supplierProductId}")
    @Operation(summary = "Update supplier product link")
    @PreAuthorize("hasAuthority('purchase.create')")
    public ErpApiResponse<SupplierDtos.SupplierProductResponse> updateSupplierProduct(
            @RequestParam Long organizationId,
            @PathVariable Long supplierId,
            @PathVariable Long supplierProductId,
            @Valid @RequestBody SupplierDtos.UpsertSupplierProductRequest request
    ) {
        return ErpApiResponse.ok(
                supplierManagementService.upsertSupplierProduct(organizationId, supplierId, supplierProductId, request),
                "Supplier product updated"
        );
    }

    @GetMapping("/{supplierId}/terms")
    @Operation(summary = "Get store-supplier terms")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<SupplierDtos.StoreSupplierTermsResponse> getTerms(
            @RequestParam Long organizationId,
            @PathVariable Long supplierId
    ) {
        return ErpApiResponse.ok(supplierManagementService.getStoreSupplierTerms(organizationId, supplierId));
    }

    @PutMapping("/{supplierId}/terms")
    @Operation(summary = "Upsert store-supplier terms")
    @PreAuthorize("hasAuthority('purchase.create')")
    public ErpApiResponse<SupplierDtos.StoreSupplierTermsResponse> upsertTerms(
            @RequestParam Long organizationId,
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierDtos.UpsertStoreSupplierTermsRequest request
    ) {
        return ErpApiResponse.ok(
                supplierManagementService.upsertStoreSupplierTerms(organizationId, supplierId, request),
                "Store supplier terms updated"
        );
    }

    @GetMapping("/product-preferences/{storeProductId}")
    @Operation(summary = "Get preferred supplier path for a store product")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<SupplierDtos.StoreProductSupplierPreferenceResponse> getStoreProductSupplierPreference(
            @RequestParam Long organizationId,
            @PathVariable Long storeProductId
    ) {
        return ErpApiResponse.ok(supplierManagementService.getStoreProductSupplierPreference(organizationId, storeProductId));
    }

    @PutMapping("/product-preferences/{storeProductId}")
    @Operation(summary = "Upsert preferred supplier path for a store product")
    @PreAuthorize("hasAuthority('purchase.create')")
    public ErpApiResponse<SupplierDtos.StoreProductSupplierPreferenceResponse> upsertStoreProductSupplierPreference(
            @RequestParam Long organizationId,
            @PathVariable Long storeProductId,
            @Valid @RequestBody SupplierDtos.UpsertStoreProductSupplierPreferenceRequest request
    ) {
        return ErpApiResponse.ok(
                supplierManagementService.upsertStoreProductSupplierPreference(organizationId, storeProductId, request),
                "Store product supplier preference updated"
        );
    }
}
