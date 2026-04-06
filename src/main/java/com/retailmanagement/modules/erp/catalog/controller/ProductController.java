package com.retailmanagement.modules.erp.catalog.controller;

import com.retailmanagement.modules.erp.catalog.dto.ProductAttributeDtos;
import com.retailmanagement.modules.erp.catalog.dto.ProductScanResponse;
import com.retailmanagement.modules.erp.catalog.dto.ProductDtos;
import com.retailmanagement.modules.erp.catalog.entity.Brand;
import com.retailmanagement.modules.erp.catalog.entity.Category;
import com.retailmanagement.modules.erp.catalog.entity.Product;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.entity.TaxGroup;
import com.retailmanagement.modules.erp.catalog.entity.Uom;
import com.retailmanagement.modules.erp.catalog.service.CatalogReferenceService;
import com.retailmanagement.modules.erp.catalog.service.ProductAttributeService;
import com.retailmanagement.modules.erp.catalog.service.ProductService;
import com.retailmanagement.modules.erp.catalog.service.StoreProductPricingService;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.party.dto.SupplierDtos;
import com.retailmanagement.modules.erp.party.service.SupplierManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController("erpProductController") @RequestMapping("/api/erp/products") @RequiredArgsConstructor
@Tag(name = "ERP Catalog", description = "ERP product and store product endpoints")
public class ProductController {
 private final ProductService service;
 private final CatalogReferenceService catalogReferenceService;
 private final ProductAttributeService productAttributeService;
 private final StoreProductPricingService pricingService;
 private final SupplierManagementService supplierManagementService;
 @GetMapping @Operation(summary = "List store products for an organization") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<List<ProductDtos.StoreProductResponse>> list(@RequestParam Long organizationId){ List<StoreProduct> products = service.listByOrganization(organizationId); return ErpApiResponse.ok(products.stream().map(product -> toStoreProductResponse(product, products)).toList()); }
 @GetMapping("/{id}") @Operation(summary = "Get store product by id") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<ProductDtos.StoreProductResponse> get(@PathVariable Long id){ StoreProduct storeProduct = service.get(id); return ErpApiResponse.ok(toStoreProductResponse(storeProduct, java.util.List.of(storeProduct))); }
 @GetMapping("/scan") @Operation(summary = "Resolve a scan query to product, batch, or serial context") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<ProductScanResponse> scan(@RequestParam Long organizationId, @RequestParam(required = false) Long warehouseId, @RequestParam String query){ return ErpApiResponse.ok(service.scan(organizationId, warehouseId, query)); }
 @GetMapping("/catalog") @Operation(summary = "Search shared products") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<List<ProductDtos.ProductResponse>> catalog(@RequestParam(required = false) String query){ return ErpApiResponse.ok(service.searchCatalog(query).stream().map(this::toProductResponse).toList()); }
 @GetMapping("/tax-group-suggestion") @Operation(summary = "Suggest tax group from HSN for an organization") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<ProductDtos.TaxGroupSuggestionResponse> suggestTaxGroup(@RequestParam Long organizationId, @RequestParam String hsnCode, @RequestParam(required = false) LocalDate effectiveDate){ return ErpApiResponse.ok(service.suggestTaxGroup(organizationId, hsnCode, effectiveDate)); }
 @GetMapping("/discover") @Operation(summary = "Search shared products not yet linked to the organization") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<List<ProductDtos.DiscoverableProductResponse>> discover(@RequestParam Long organizationId, @RequestParam(required = false) String query){ return ErpApiResponse.ok(service.discoverCatalog(organizationId, query).stream().map(this::toDiscoverableProductResponse).toList()); }
 @GetMapping("/catalog/{id}") @Operation(summary = "Get shared product by id") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<ProductDtos.ProductResponse> catalogProduct(@PathVariable Long id){ return ErpApiResponse.ok(toProductResponse(service.getCatalogProduct(id))); }
 @PostMapping @Operation(summary = "Create a store product association") @PreAuthorize("hasAuthority('catalog.manage')") public ErpApiResponse<ProductDtos.StoreProductResponse> create(@RequestBody ProductDtos.CreateStoreProductRequest request){ StoreProduct storeProduct = service.create(toStoreProduct(request), request.hsnCode(), request.attributes()); return ErpApiResponse.ok(toStoreProductResponse(storeProduct, java.util.List.of(storeProduct)), "ERP store product created"); }
 @PutMapping("/{id}") @Operation(summary = "Update an existing store product") @PreAuthorize("hasAuthority('catalog.manage')") public ErpApiResponse<ProductDtos.StoreProductResponse> update(@PathVariable Long id, @RequestBody ProductDtos.CreateStoreProductRequest request){ StoreProduct storeProduct = service.update(id, toStoreProduct(request), request.attributes()); return ErpApiResponse.ok(toStoreProductResponse(storeProduct, java.util.List.of(storeProduct)), "ERP store product updated"); }
 @PostMapping("/link") @Operation(summary = "Link a shared product into store product catalog") @PreAuthorize("hasAuthority('catalog.manage')") public ErpApiResponse<ProductDtos.StoreProductResponse> link(@RequestBody ProductDtos.LinkCatalogProductRequest request){ StoreProduct storeProduct = service.linkCatalogProduct(request); if (request.attributes() != null) { productAttributeService.replaceValuesForStoreProduct(storeProduct, request.attributes()); } return ErpApiResponse.ok(toStoreProductResponse(storeProduct, java.util.List.of(storeProduct)), "Shared product linked to store catalog"); }
 @GetMapping("/{id}/suppliers") @Operation(summary = "Get linked suppliers for a store product") @PreAuthorize("hasAuthority('purchase.view')") public ErpApiResponse<SupplierDtos.StoreProductSuppliersResponse> getSuppliers(@PathVariable Long id, @RequestParam Long organizationId){ return ErpApiResponse.ok(supplierManagementService.getStoreProductSuppliers(organizationId, id)); }
 @PutMapping("/{id}/suppliers") @Operation(summary = "Upsert linked suppliers for a store product") @PreAuthorize("hasAuthority('purchase.create')") public ErpApiResponse<SupplierDtos.StoreProductSuppliersResponse> upsertSuppliers(@PathVariable Long id, @RequestParam Long organizationId, @RequestBody SupplierDtos.UpsertStoreProductSuppliersRequest request){ return ErpApiResponse.ok(supplierManagementService.upsertStoreProductSuppliers(organizationId, id, request), "Store product suppliers updated"); }
 @GetMapping("/{id}/prices") @Operation(summary = "List store product prices") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<List<ProductDtos.StoreProductPriceResponse>> listPrices(@PathVariable Long id, @RequestParam Long organizationId){ return ErpApiResponse.ok(pricingService.listPrices(organizationId, id)); }
 @PostMapping("/{id}/prices") @Operation(summary = "Create store product price") @PreAuthorize("hasAuthority('catalog.manage')") public ErpApiResponse<ProductDtos.StoreProductPriceResponse> createPrice(@PathVariable Long id, @RequestParam Long organizationId, @RequestBody ProductDtos.UpsertStoreProductPriceRequest request){ return ErpApiResponse.ok(pricingService.createPrice(organizationId, id, request), "Store product price created"); }
 @PutMapping("/{id}/prices/{priceId}") @Operation(summary = "Update store product price") @PreAuthorize("hasAuthority('catalog.manage')") public ErpApiResponse<ProductDtos.StoreProductPriceResponse> updatePrice(@PathVariable Long id, @PathVariable Long priceId, @RequestParam Long organizationId, @RequestBody ProductDtos.UpsertStoreProductPriceRequest request){ return ErpApiResponse.ok(pricingService.updatePrice(organizationId, id, priceId, request), "Store product price updated"); }

 private ProductDtos.StoreProductResponse toStoreProductResponse(StoreProduct storeProduct, List<StoreProduct> scope) {
  java.util.Set<Long> categoryIds = scope.stream().map(StoreProduct::getCategoryId).collect(java.util.stream.Collectors.toSet());
 java.util.Set<Long> brandIds = scope.stream().map(StoreProduct::getBrandId).collect(java.util.stream.Collectors.toSet());
 java.util.Set<Long> uomIds = scope.stream().map(StoreProduct::getBaseUomId).collect(java.util.stream.Collectors.toSet());
 java.util.Set<Long> taxGroupIds = scope.stream().map(StoreProduct::getTaxGroupId).collect(java.util.stream.Collectors.toSet());
  java.util.List<Long> storeProductIds = scope.stream().map(StoreProduct::getId).toList();
  java.util.Map<Long, Category> categories = catalogReferenceService.categoriesByIds(categoryIds);
  java.util.Map<Long, Brand> brands = catalogReferenceService.brandsByIds(brandIds);
  java.util.Map<Long, Uom> uoms = catalogReferenceService.uomsByIds(uomIds);
  java.util.Map<Long, TaxGroup> taxGroups = catalogReferenceService.taxGroupsByIds(taxGroupIds);
  java.util.Map<Long, java.util.List<ProductAttributeDtos.ProductAttributeValueResponse>> attributeValues =
          productAttributeService.valuesByStoreProductIds(storeProduct.getOrganizationId(), storeProductIds);
  Category category = categories.get(storeProduct.getCategoryId());
  Brand brand = brands.get(storeProduct.getBrandId());
  Uom uom = uoms.get(storeProduct.getBaseUomId());
  TaxGroup taxGroup = taxGroups.get(storeProduct.getTaxGroupId());
  return new ProductDtos.StoreProductResponse(
          storeProduct.getId(),
          storeProduct.getOrganizationId(),
          storeProduct.getProductId(),
          storeProduct.getCategoryId(),
          category != null ? category.getName() : null,
          storeProduct.getBrandId(),
          brand != null ? brand.getName() : null,
          storeProduct.getBaseUomId(),
          uom != null ? uom.getCode() : null,
          uom != null ? uom.getName() : null,
          storeProduct.getTaxGroupId(),
          taxGroup != null ? taxGroup.getCode() : null,
          taxGroup != null ? taxGroup.getName() : null,
          attributeValues.getOrDefault(storeProduct.getId(), java.util.List.of()),
          storeProduct.getSku(),
          storeProduct.getName(),
          storeProduct.getDescription(),
          storeProduct.getInventoryTrackingMode(),
          storeProduct.getSerialTrackingEnabled(),
          storeProduct.getBatchTrackingEnabled(),
          storeProduct.getExpiryTrackingEnabled(),
          storeProduct.getFractionalQuantityAllowed(),
          storeProduct.getMinStockBaseQty(),
          storeProduct.getReorderLevelBaseQty(),
          storeProduct.getDefaultSalePrice(),
          storeProduct.getDefaultWarrantyMonths(),
          storeProduct.getWarrantyTerms(),
          storeProduct.getIsServiceItem(),
          storeProduct.getIsActive(),
          storeProduct.getCreatedAt(),
          storeProduct.getUpdatedAt()
  );
 }

 private ProductDtos.ProductResponse toProductResponse(Product product) {
  return new ProductDtos.ProductResponse(
          product.getId(),
          product.getName(),
          product.getDescription(),
          product.getCategoryName(),
          product.getBrandName(),
          product.getHsnCode(),
          product.getBaseUomId(),
          product.getInventoryTrackingMode(),
          product.getSerialTrackingEnabled(),
          product.getBatchTrackingEnabled(),
          product.getExpiryTrackingEnabled(),
          product.getFractionalQuantityAllowed(),
          product.getIsServiceItem(),
          product.getIsActive(),
          product.getCreatedAt(),
          product.getUpdatedAt()
  );
 }

 private ProductDtos.DiscoverableProductResponse toDiscoverableProductResponse(Product product) {
  return new ProductDtos.DiscoverableProductResponse(
          product.getId(),
          product.getName(),
          product.getDescription(),
          product.getCategoryName(),
          product.getBrandName(),
          product.getHsnCode(),
          product.getBaseUomId(),
          product.getInventoryTrackingMode(),
          product.getSerialTrackingEnabled(),
          product.getBatchTrackingEnabled(),
          product.getExpiryTrackingEnabled(),
          product.getFractionalQuantityAllowed(),
          product.getIsServiceItem(),
          product.getIsActive()
  );
 }

 private StoreProduct toStoreProduct(ProductDtos.CreateStoreProductRequest request) {
  StoreProduct storeProduct = new StoreProduct();
  storeProduct.setOrganizationId(request.organizationId());
  storeProduct.setProductId(request.productId());
  storeProduct.setCategoryId(request.categoryId());
  storeProduct.setBrandId(request.brandId());
  storeProduct.setBaseUomId(request.baseUomId());
  storeProduct.setTaxGroupId(request.taxGroupId());
  storeProduct.setSku(request.sku());
  storeProduct.setName(request.name());
  storeProduct.setDescription(request.description());
  storeProduct.setInventoryTrackingMode(request.inventoryTrackingMode());
  storeProduct.setSerialTrackingEnabled(Boolean.TRUE.equals(request.serialTrackingEnabled()));
  storeProduct.setBatchTrackingEnabled(Boolean.TRUE.equals(request.batchTrackingEnabled()));
  storeProduct.setExpiryTrackingEnabled(Boolean.TRUE.equals(request.expiryTrackingEnabled()));
  storeProduct.setFractionalQuantityAllowed(Boolean.TRUE.equals(request.fractionalQuantityAllowed()));
  if (request.minStockBaseQty() != null) {
   storeProduct.setMinStockBaseQty(request.minStockBaseQty());
  }
  if (request.reorderLevelBaseQty() != null) {
   storeProduct.setReorderLevelBaseQty(request.reorderLevelBaseQty());
  }
  if (request.defaultSalePrice() != null) {
   storeProduct.setDefaultSalePrice(request.defaultSalePrice());
  }
  storeProduct.setDefaultWarrantyMonths(request.defaultWarrantyMonths());
  storeProduct.setWarrantyTerms(request.warrantyTerms());
  storeProduct.setIsServiceItem(Boolean.TRUE.equals(request.isServiceItem()));
  storeProduct.setIsActive(request.isActive() == null || request.isActive());
  return storeProduct;
 }
}
