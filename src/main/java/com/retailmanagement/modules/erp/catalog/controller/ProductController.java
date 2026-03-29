package com.retailmanagement.modules.erp.catalog.controller;

import com.retailmanagement.modules.erp.catalog.dto.ProductScanResponse;
import com.retailmanagement.modules.erp.catalog.dto.ProductDtos;
import com.retailmanagement.modules.erp.catalog.entity.Product;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.service.ProductService;
import com.retailmanagement.modules.erp.catalog.service.StoreProductPricingService;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController("erpProductController") @RequestMapping("/api/erp/products") @RequiredArgsConstructor
@Tag(name = "ERP Catalog", description = "ERP product and store product endpoints")
public class ProductController {
 private final ProductService service;
 private final StoreProductPricingService pricingService;
 @GetMapping @Operation(summary = "List store products for an organization") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<List<ProductDtos.StoreProductResponse>> list(@RequestParam Long organizationId){ return ErpApiResponse.ok(service.listByOrganization(organizationId).stream().map(this::toStoreProductResponse).toList()); }
 @GetMapping("/{id}") @Operation(summary = "Get store product by id") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<ProductDtos.StoreProductResponse> get(@PathVariable Long id){ return ErpApiResponse.ok(toStoreProductResponse(service.get(id))); }
 @GetMapping("/scan") @Operation(summary = "Resolve a scan query to product, batch, or serial context") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<ProductScanResponse> scan(@RequestParam Long organizationId, @RequestParam(required = false) Long warehouseId, @RequestParam String query){ return ErpApiResponse.ok(service.scan(organizationId, warehouseId, query)); }
 @GetMapping("/catalog") @Operation(summary = "Search shared products") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<List<ProductDtos.ProductResponse>> catalog(@RequestParam(required = false) String query){ return ErpApiResponse.ok(service.searchCatalog(query).stream().map(this::toProductResponse).toList()); }
 @GetMapping("/discover") @Operation(summary = "Search shared products not yet linked to the organization") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<List<ProductDtos.DiscoverableProductResponse>> discover(@RequestParam Long organizationId, @RequestParam(required = false) String query){ return ErpApiResponse.ok(service.discoverCatalog(organizationId, query).stream().map(this::toDiscoverableProductResponse).toList()); }
 @GetMapping("/catalog/{id}") @Operation(summary = "Get shared product by id") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<ProductDtos.ProductResponse> catalogProduct(@PathVariable Long id){ return ErpApiResponse.ok(toProductResponse(service.getCatalogProduct(id))); }
 @PostMapping @Operation(summary = "Create a store product association") @PreAuthorize("hasAuthority('catalog.manage')") public ErpApiResponse<ProductDtos.StoreProductResponse> create(@RequestBody ProductDtos.CreateStoreProductRequest request){ return ErpApiResponse.ok(toStoreProductResponse(service.create(toStoreProduct(request), request.hsnCode())), "ERP store product created"); }
 @PostMapping("/link") @Operation(summary = "Link a shared product into store product catalog") @PreAuthorize("hasAuthority('catalog.manage')") public ErpApiResponse<ProductDtos.StoreProductResponse> link(@RequestBody ProductDtos.LinkCatalogProductRequest request){ return ErpApiResponse.ok(toStoreProductResponse(service.linkCatalogProduct(request)), "Shared product linked to store catalog"); }
 @GetMapping("/{id}/prices") @Operation(summary = "List store product prices") @PreAuthorize("hasAuthority('catalog.view')") public ErpApiResponse<List<ProductDtos.StoreProductPriceResponse>> listPrices(@PathVariable Long id, @RequestParam Long organizationId){ return ErpApiResponse.ok(pricingService.listPrices(organizationId, id)); }
 @PostMapping("/{id}/prices") @Operation(summary = "Create store product price") @PreAuthorize("hasAuthority('catalog.manage')") public ErpApiResponse<ProductDtos.StoreProductPriceResponse> createPrice(@PathVariable Long id, @RequestParam Long organizationId, @RequestBody ProductDtos.UpsertStoreProductPriceRequest request){ return ErpApiResponse.ok(pricingService.createPrice(organizationId, id, request), "Store product price created"); }
 @PutMapping("/{id}/prices/{priceId}") @Operation(summary = "Update store product price") @PreAuthorize("hasAuthority('catalog.manage')") public ErpApiResponse<ProductDtos.StoreProductPriceResponse> updatePrice(@PathVariable Long id, @PathVariable Long priceId, @RequestParam Long organizationId, @RequestBody ProductDtos.UpsertStoreProductPriceRequest request){ return ErpApiResponse.ok(pricingService.updatePrice(organizationId, id, priceId, request), "Store product price updated"); }

 private ProductDtos.StoreProductResponse toStoreProductResponse(StoreProduct storeProduct) {
  return new ProductDtos.StoreProductResponse(
          storeProduct.getId(),
          storeProduct.getOrganizationId(),
          storeProduct.getProductId(),
          storeProduct.getCategoryId(),
          storeProduct.getBrandId(),
          storeProduct.getBaseUomId(),
          storeProduct.getTaxGroupId(),
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
  storeProduct.setIsServiceItem(Boolean.TRUE.equals(request.isServiceItem()));
  storeProduct.setIsActive(request.isActive() == null || request.isActive());
  return storeProduct;
 }
}
