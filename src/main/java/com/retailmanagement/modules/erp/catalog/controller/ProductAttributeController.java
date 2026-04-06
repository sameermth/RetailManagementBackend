package com.retailmanagement.modules.erp.catalog.controller;

import com.retailmanagement.modules.erp.catalog.dto.ProductAttributeDtos;
import com.retailmanagement.modules.erp.catalog.service.ProductAttributeService;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/catalog/attributes")
@RequiredArgsConstructor
@Tag(name = "ERP Catalog", description = "Dynamic product attribute metadata endpoints")
public class ProductAttributeController {

    private final ProductAttributeService productAttributeService;

    @GetMapping
    @Operation(summary = "List product attributes for dynamic product forms")
    @PreAuthorize("hasAuthority('catalog.view')")
    public ErpApiResponse<List<ProductAttributeDtos.ProductAttributeDefinitionResponse>> list(@RequestParam Long organizationId,
                                                                                              @RequestParam(required = false) Long categoryId,
                                                                                              @RequestParam(required = false) Long brandId) {
        return ErpApiResponse.ok(productAttributeService.list(organizationId, categoryId, brandId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product attribute definition")
    @PreAuthorize("hasAuthority('catalog.view')")
    public ErpApiResponse<ProductAttributeDtos.ProductAttributeDefinitionResponse> get(@PathVariable Long id,
                                                                                       @RequestParam Long organizationId) {
        return ErpApiResponse.ok(productAttributeService.get(id, organizationId));
    }

    @GetMapping("/ui-config")
    @Operation(summary = "Get UI config for dynamic product attribute builders")
    @PreAuthorize("hasAuthority('catalog.view')")
    public ErpApiResponse<ProductAttributeDtos.ProductAttributeUiConfigResponse> uiConfig(@RequestParam Long organizationId) {
        return ErpApiResponse.ok(productAttributeService.uiConfig(organizationId));
    }

    @PostMapping
    @Operation(summary = "Create product attribute definition")
    @PreAuthorize("hasAuthority('catalog.manage')")
    public ErpApiResponse<ProductAttributeDtos.ProductAttributeDefinitionResponse> create(@Valid @RequestBody ProductAttributeDtos.UpsertProductAttributeDefinitionRequest request) {
        return ErpApiResponse.ok(productAttributeService.create(request), "Product attribute definition created");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product attribute definition")
    @PreAuthorize("hasAuthority('catalog.manage')")
    public ErpApiResponse<ProductAttributeDtos.ProductAttributeDefinitionResponse> update(@PathVariable Long id,
                                                                                          @Valid @RequestBody ProductAttributeDtos.UpsertProductAttributeDefinitionRequest request) {
        return ErpApiResponse.ok(productAttributeService.update(id, request), "Product attribute definition updated");
    }
}
