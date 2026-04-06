package com.retailmanagement.modules.erp.catalog.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.dto.ProductAttributeDtos;
import com.retailmanagement.modules.erp.catalog.dto.ProductScanResponse;
import com.retailmanagement.modules.erp.catalog.dto.ProductDtos;
import com.retailmanagement.modules.erp.catalog.entity.Brand;
import com.retailmanagement.modules.erp.catalog.entity.Category;
import com.retailmanagement.modules.erp.catalog.entity.Product;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.entity.TaxGroup;
import com.retailmanagement.modules.erp.catalog.repository.BrandRepository;
import com.retailmanagement.modules.erp.catalog.repository.CategoryRepository;
import com.retailmanagement.modules.erp.catalog.repository.ProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.TaxGroupRepository;
import com.retailmanagement.modules.erp.catalog.repository.UomRepository;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBatchRepository;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class ProductService {
 private final StoreProductRepository storeProductRepository;
 private final ProductRepository productRepository;
 private final BrandRepository brandRepository;
 private final CategoryRepository categoryRepository;
 private final UomRepository uomRepository;
 private final HsnMasterService hsnMasterService;
 private final TaxGroupRepository taxGroupRepository;
 private final ProductAttributeService productAttributeService;
 private final ErpAccessGuard accessGuard;
 private final InventoryBalanceRepository inventoryBalanceRepository;
 private final InventoryBatchRepository inventoryBatchRepository;
 private final SerialNumberRepository serialNumberRepository;

 @Transactional(readOnly=true)
 public List<StoreProduct> listByOrganization(Long organizationId){
  accessGuard.assertOrganizationAccess(organizationId);
  return storeProductRepository.findByOrganizationId(organizationId);
 }

 @Transactional(readOnly=true)
 public StoreProduct get(Long id){
  StoreProduct storeProduct = storeProductRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ERP store product not found: "+id));
  accessGuard.assertOrganizationAccess(storeProduct.getOrganizationId());
  return storeProduct;
 }

 @Transactional(readOnly=true)
 public List<Product> searchCatalog(String query) {
  String q = query == null ? "" : query.trim();
  if (q.isBlank()) {
   return productRepository.findAll().stream().limit(20).toList();
  }
  return productRepository.findTop20ByNameContainingIgnoreCaseOrHsnCodeContainingIgnoreCase(q, q);
 }

 @Transactional(readOnly=true)
 public Product getCatalogProduct(Long id) {
  return productRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
 }

 @Transactional(readOnly=true)
 public List<Product> discoverCatalog(Long organizationId, String query) {
  accessGuard.assertOrganizationAccess(organizationId);
  List<Product> catalog = searchCatalog(query);
  java.util.Set<Long> linkedProductIds = storeProductRepository.findByOrganizationId(organizationId).stream()
          .map(StoreProduct::getProductId)
          .collect(java.util.stream.Collectors.toSet());
  return catalog.stream()
          .filter(product -> !linkedProductIds.contains(product.getId()))
          .toList();
 }

 @Transactional(readOnly=true)
 public ProductScanResponse scan(Long organizationId, Long warehouseId, String query) {
  accessGuard.assertOrganizationAccess(organizationId);
  String lookup = trimToNull(query);
  if (lookup == null) {
   throw new BusinessException("Scan/search value is required");
  }

  Optional<SerialNumber> serialMatch = serialNumberRepository.findFirstByOrganizationIdAndSerialNumberIgnoreCase(organizationId, lookup)
          .or(() -> serialNumberRepository.findFirstByOrganizationIdAndManufacturerSerialNumberIgnoreCase(organizationId, lookup));
  if (serialMatch.isPresent()) {
   SerialNumber serial = serialMatch.get();
   StoreProduct storeProduct = storeProductRepository.findById(serial.getProductId())
           .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + serial.getProductId()));
   return buildScanResponse("SERIAL", storeProduct, serial.getBatchId(), serial, warehouseId);
  }

  Optional<InventoryBatch> batchMatch = inventoryBatchRepository.findFirstByOrganizationIdAndBatchNumberIgnoreCase(organizationId, lookup)
          .or(() -> inventoryBatchRepository.findFirstByOrganizationIdAndManufacturerBatchNumberIgnoreCase(organizationId, lookup));
  if (batchMatch.isPresent()) {
   InventoryBatch batch = batchMatch.get();
   StoreProduct storeProduct = storeProductRepository.findById(batch.getProductId())
           .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + batch.getProductId()));
   return buildScanResponse("BATCH", storeProduct, batch.getId(), null, warehouseId);
  }

  Optional<StoreProduct> productMatch = storeProductRepository.findFirstByOrganizationIdAndSkuIgnoreCase(organizationId, lookup);
  if (productMatch.isPresent()) {
   return buildScanResponse("PRODUCT_CODE", productMatch.get(), null, null, warehouseId);
  }

  throw new ResourceNotFoundException("No store product found for scan/search value: " + lookup);
 }

 public StoreProduct create(StoreProduct storeProduct, String hsnCode, List<ProductAttributeDtos.UpsertProductAttributeValueRequest> attributes){
  accessGuard.assertOrganizationAccess(storeProduct.getOrganizationId());
  storeProduct.setTaxGroupId(resolveRequestedOrSuggestedTaxGroupId(storeProduct.getOrganizationId(), hsnCode, storeProduct.getTaxGroupId()));
  validateReferences(storeProduct);
  Product product = resolveProduct(storeProduct, hsnCode);

  Optional<StoreProduct> existingAssociation = storeProductRepository.findByOrganizationIdAndProductId(storeProduct.getOrganizationId(), product.getId());
  if (existingAssociation.isPresent()) {
   StoreProduct existing = existingAssociation.get();
   if (storeProduct.getSku() != null && !storeProduct.getSku().isBlank() && !storeProduct.getSku().equalsIgnoreCase(existing.getSku())) {
    throw new BusinessException("This product is already associated with the organization as SKU " + existing.getSku());
   }
   return existing;
  }

  storeProductRepository.findByOrganizationIdAndSku(storeProduct.getOrganizationId(), storeProduct.getSku())
          .ifPresent(existing -> {
           throw new BusinessException("Product SKU already exists in organization: " + existing.getSku());
          });

  storeProduct.setProductId(product.getId());
  StoreProduct saved = storeProductRepository.save(storeProduct);
  productAttributeService.replaceValuesForStoreProduct(saved, attributes);
  return saved;
 }

 public StoreProduct update(Long id, StoreProduct updates, List<ProductAttributeDtos.UpsertProductAttributeValueRequest> attributes) {
  StoreProduct existing = get(id);
  if (!existing.getOrganizationId().equals(updates.getOrganizationId())) {
   throw new BusinessException("Store product does not belong to organization " + updates.getOrganizationId() + ": " + id);
  }

  storeProductRepository.findByOrganizationIdAndSku(existing.getOrganizationId(), updates.getSku())
          .filter(other -> !other.getId().equals(existing.getId()))
          .ifPresent(other -> {
           throw new BusinessException("Product SKU already exists in organization: " + other.getSku());
          });

  existing.setCategoryId(updates.getCategoryId());
  existing.setBrandId(updates.getBrandId());
  existing.setBaseUomId(updates.getBaseUomId());
  if (updates.getTaxGroupId() != null) {
   existing.setTaxGroupId(updates.getTaxGroupId());
  }
  existing.setSku(updates.getSku());
  existing.setName(updates.getName());
  existing.setDescription(trimToNull(updates.getDescription()));
  existing.setInventoryTrackingMode(updates.getInventoryTrackingMode());
  existing.setSerialTrackingEnabled(booleanValue(updates.getSerialTrackingEnabled()));
  existing.setBatchTrackingEnabled(booleanValue(updates.getBatchTrackingEnabled()));
  existing.setExpiryTrackingEnabled(booleanValue(updates.getExpiryTrackingEnabled()));
  existing.setFractionalQuantityAllowed(booleanValue(updates.getFractionalQuantityAllowed()));
  existing.setMinStockBaseQty(updates.getMinStockBaseQty() == null ? BigDecimal.ZERO : updates.getMinStockBaseQty());
  existing.setReorderLevelBaseQty(updates.getReorderLevelBaseQty() == null ? BigDecimal.ZERO : updates.getReorderLevelBaseQty());
  existing.setDefaultSalePrice(updates.getDefaultSalePrice());
  existing.setDefaultWarrantyMonths(updates.getDefaultWarrantyMonths());
  existing.setWarrantyTerms(trimToNull(updates.getWarrantyTerms()));
  existing.setIsServiceItem(booleanValue(updates.getIsServiceItem()));
  existing.setIsActive(updates.getIsActive() == null || updates.getIsActive());

  validateReferences(existing);
  StoreProduct saved = storeProductRepository.save(existing);
  productAttributeService.replaceValuesForStoreProduct(saved, attributes);
  return saved;
 }

 public StoreProduct linkCatalogProduct(ProductDtos.LinkCatalogProductRequest request) {
  accessGuard.assertOrganizationAccess(request.organizationId());
  Product product = productRepository.findById(request.productId())
          .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
  storeProductRepository.findByOrganizationIdAndProductId(request.organizationId(), product.getId())
          .ifPresent(existing -> {
           throw new BusinessException("Product is already linked to this organization as store product " + existing.getId());
          });

  StoreProduct storeProduct = new StoreProduct();
  storeProduct.setOrganizationId(request.organizationId());
  storeProduct.setProductId(product.getId());
  storeProduct.setCategoryId(request.categoryId());
  storeProduct.setBrandId(request.brandId());
  storeProduct.setBaseUomId(product.getBaseUomId());
  storeProduct.setTaxGroupId(resolveRequestedOrSuggestedTaxGroupId(request.organizationId(), product.getHsnCode(), request.taxGroupId()));
  storeProduct.setSku(trimToNull(request.sku()) != null ? trimToNull(request.sku()) : fallbackSku(product));
  storeProduct.setName(trimToNull(request.name()) != null ? trimToNull(request.name()) : product.getName());
  storeProduct.setDescription(trimToNull(request.description()) != null ? trimToNull(request.description()) : trimToNull(product.getDescription()));
  storeProduct.setInventoryTrackingMode(product.getInventoryTrackingMode());
  storeProduct.setSerialTrackingEnabled(booleanValue(product.getSerialTrackingEnabled()));
  storeProduct.setBatchTrackingEnabled(booleanValue(product.getBatchTrackingEnabled()));
  storeProduct.setExpiryTrackingEnabled(booleanValue(product.getExpiryTrackingEnabled()));
  storeProduct.setFractionalQuantityAllowed(booleanValue(product.getFractionalQuantityAllowed()));
  if (request.minStockBaseQty() != null) {
   storeProduct.setMinStockBaseQty(request.minStockBaseQty());
  }
  if (request.reorderLevelBaseQty() != null) {
   storeProduct.setReorderLevelBaseQty(request.reorderLevelBaseQty());
  }
  if (request.defaultSalePrice() != null) {
   storeProduct.setDefaultSalePrice(request.defaultSalePrice());
  }
  storeProduct.setIsServiceItem(booleanValue(product.getIsServiceItem()));
  storeProduct.setIsActive(request.isActive() == null ? booleanValue(product.getIsActive()) : request.isActive());
  validateReferences(storeProduct);
  storeProductRepository.findByOrganizationIdAndSku(storeProduct.getOrganizationId(), storeProduct.getSku())
          .ifPresent(existing -> {
           throw new BusinessException("Product SKU already exists in organization: " + existing.getSku());
          });
  return storeProductRepository.save(storeProduct);
 }

 @Transactional(readOnly = true)
 public ProductDtos.TaxGroupSuggestionResponse suggestTaxGroup(Long organizationId, String hsnCode, LocalDate effectiveDate) {
  accessGuard.assertOrganizationAccess(organizationId);
  String normalizedHsnCode = validateHsnCode(hsnCode);
  if (normalizedHsnCode == null) {
   throw new BusinessException("HSN code is required");
  }
  HsnMasterService.HsnReference reference = hsnMasterService.getResolvedByCode(normalizedHsnCode, effectiveDate);
  TaxGroup taxGroup = findSuggestedTaxGroup(organizationId, reference);
  if (taxGroup == null) {
   return new ProductDtos.TaxGroupSuggestionResponse(
           normalizedHsnCode,
           null,
           null,
           null,
           rate(reference, true, "cgst"),
           rate(reference, true, "sgst"),
           rate(reference, true, "igst"),
           rate(reference, true, "cess"),
           reference.taxRate() != null ? reference.taxRate().getEffectiveFrom() : reference.master().getEffectiveFrom(),
           false,
           "No active tax group in this organization matches the selected HSN rate"
   );
  }
  return new ProductDtos.TaxGroupSuggestionResponse(
          normalizedHsnCode,
          taxGroup.getId(),
          taxGroup.getCode(),
          taxGroup.getName(),
          taxGroup.getCgstRate(),
          taxGroup.getSgstRate(),
          taxGroup.getIgstRate(),
          taxGroup.getCessRate(),
          reference.taxRate() != null ? reference.taxRate().getEffectiveFrom() : reference.master().getEffectiveFrom(),
          true,
          "Suggested from HSN rate"
  );
 }

 private void validateReferences(StoreProduct storeProduct) {
  uomRepository.findById(storeProduct.getBaseUomId())
          .orElseThrow(() -> new ResourceNotFoundException("UOM not found: " + storeProduct.getBaseUomId()));
  categoryRepository.findById(storeProduct.getCategoryId())
          .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + storeProduct.getCategoryId()));
  brandRepository.findById(storeProduct.getBrandId())
          .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + storeProduct.getBrandId()));
  taxGroupRepository.findByIdAndOrganizationId(storeProduct.getTaxGroupId(), storeProduct.getOrganizationId())
          .orElseThrow(() -> new ResourceNotFoundException("Tax group not found: " + storeProduct.getTaxGroupId()));
 }

 private Product resolveProduct(StoreProduct storeProduct, String hsnCode) {
  String normalizedHsnCode = validateHsnCode(hsnCode);
  if (storeProduct.getProductId() != null) {
   Product product = productRepository.findById(storeProduct.getProductId())
           .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + storeProduct.getProductId()));
   if (normalizedHsnCode != null && product.getHsnCode() != null && !normalizedHsnCode.equals(product.getHsnCode())) {
    throw new BusinessException("Selected catalog product already uses HSN " + product.getHsnCode());
   }
   return product;
  }

  Brand brand = brandRepository.findById(storeProduct.getBrandId())
          .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + storeProduct.getBrandId()));
  Category category = categoryRepository.findById(storeProduct.getCategoryId())
          .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + storeProduct.getCategoryId()));

  return productRepository.findExactMatch(
          storeProduct.getName(),
          brand.getName(),
          storeProduct.getBaseUomId(),
          storeProduct.getInventoryTrackingMode(),
          booleanValue(storeProduct.getSerialTrackingEnabled()),
          booleanValue(storeProduct.getBatchTrackingEnabled()),
          booleanValue(storeProduct.getExpiryTrackingEnabled()),
          booleanValue(storeProduct.getFractionalQuantityAllowed()),
          booleanValue(storeProduct.getIsServiceItem())
  ).orElseGet(() -> createProduct(storeProduct, category, brand, normalizedHsnCode));
 }

 private Product createProduct(StoreProduct storeProduct, Category category, Brand brand, String hsnCode) {
  Product product = new Product();
  product.setName(storeProduct.getName());
  product.setDescription(trimToNull(storeProduct.getDescription()));
  product.setHsnCode(validateHsnCode(hsnCode));
  product.setCategoryName(category.getName());
  product.setBrandName(brand.getName());
  product.setBaseUomId(storeProduct.getBaseUomId());
  product.setInventoryTrackingMode(storeProduct.getInventoryTrackingMode());
  product.setSerialTrackingEnabled(booleanValue(storeProduct.getSerialTrackingEnabled()));
  product.setBatchTrackingEnabled(booleanValue(storeProduct.getBatchTrackingEnabled()));
  product.setExpiryTrackingEnabled(booleanValue(storeProduct.getExpiryTrackingEnabled()));
  product.setFractionalQuantityAllowed(booleanValue(storeProduct.getFractionalQuantityAllowed()));
  product.setIsServiceItem(booleanValue(storeProduct.getIsServiceItem()));
  product.setIsActive(booleanValue(storeProduct.getIsActive()));
  return productRepository.save(product);
 }

 private String validateHsnCode(String hsnCode) {
  String normalized = hsnMasterService.normalize(hsnCode);
  if (normalized == null) {
   return null;
  }
  if (!hsnMasterService.exists(normalized)) {
   throw new BusinessException("Unknown HSN code: " + normalized);
  }
  return normalized;
 }

 private Long resolveRequestedOrSuggestedTaxGroupId(Long organizationId, String hsnCode, Long requestedTaxGroupId) {
  if (requestedTaxGroupId != null) {
   return requestedTaxGroupId;
  }
  String normalizedHsnCode = validateHsnCode(hsnCode);
  if (normalizedHsnCode == null) {
   throw new BusinessException("Tax group is required when HSN is not selected");
  }
  HsnMasterService.HsnReference reference = hsnMasterService.getResolvedByCode(normalizedHsnCode, LocalDate.now());
  TaxGroup suggested = findSuggestedTaxGroup(organizationId, reference);
  if (suggested == null) {
   throw new BusinessException("No active tax group matches HSN " + normalizedHsnCode + " for this organization");
  }
  return suggested.getId();
 }

 private TaxGroup findSuggestedTaxGroup(Long organizationId, HsnMasterService.HsnReference reference) {
  BigDecimal cgstRate = rate(reference, false, "cgst");
  BigDecimal sgstRate = rate(reference, false, "sgst");
  BigDecimal igstRate = rate(reference, false, "igst");
  BigDecimal cessRate = rate(reference, false, "cess");
  List<TaxGroup> matches = taxGroupRepository.findByOrganizationIdAndIsActiveTrueAndCgstRateAndSgstRateAndIgstRateAndCessRate(
          organizationId,
          cgstRate,
          sgstRate,
          igstRate,
          cessRate
  );
  if (matches.isEmpty()) {
   return null;
  }
  return matches.stream()
          .sorted((left, right) -> {
           boolean leftPreferred = left.getCode() != null && left.getCode().startsWith("GST_");
           boolean rightPreferred = right.getCode() != null && right.getCode().startsWith("GST_");
           if (leftPreferred == rightPreferred) {
            return left.getName().compareToIgnoreCase(right.getName());
           }
           return leftPreferred ? -1 : 1;
          })
          .findFirst()
          .orElse(null);
 }

 private BigDecimal rate(HsnMasterService.HsnReference reference, boolean allowFallback, String kind) {
  if (reference.taxRate() != null) {
   return switch (kind) {
    case "cgst" -> reference.taxRate().getCgstRate();
    case "sgst" -> reference.taxRate().getSgstRate();
    case "igst" -> reference.taxRate().getIgstRate();
    case "cess" -> reference.taxRate().getCessRate();
    default -> BigDecimal.ZERO;
   };
  }
  if (allowFallback || reference.master() != null) {
   return switch (kind) {
    case "cgst" -> reference.master().getCgstRate();
    case "sgst" -> reference.master().getSgstRate();
    case "igst" -> reference.master().getIgstRate();
    case "cess" -> reference.master().getCessRate();
    default -> BigDecimal.ZERO;
   };
  }
  return BigDecimal.ZERO;
 }

 private ProductScanResponse buildScanResponse(
         String matchedBy,
         StoreProduct storeProduct,
         Long batchId,
         SerialNumber serial,
         Long warehouseId
 ) {
  Product product = productRepository.findById(storeProduct.getProductId())
          .orElse(null);
  InventoryBatch batch = batchId == null ? null : inventoryBatchRepository.findById(batchId).orElse(null);
  ProductScanResponse.StockSnapshot stock = stockSnapshot(storeProduct.getOrganizationId(), storeProduct.getId(), warehouseId, batchId);
  return new ProductScanResponse(
          matchedBy,
          new ProductScanResponse.StoreProductSummary(
                  storeProduct.getId(),
                  storeProduct.getProductId(),
                  storeProduct.getOrganizationId(),
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
                  storeProduct.getIsServiceItem(),
                  storeProduct.getIsActive()
          ),
          product == null ? null : new ProductScanResponse.ProductSummary(
                  product.getId(),
                  product.getName(),
                  product.getDescription(),
                  product.getCategoryName(),
                  product.getBrandName(),
                  product.getHsnCode(),
                  product.getBaseUomId(),
                  product.getInventoryTrackingMode()
          ),
          serial == null ? null : new ProductScanResponse.SerialMatch(
                  serial.getId(),
                  serial.getSerialNumber(),
                  serial.getManufacturerSerialNumber(),
                  serial.getStatus(),
                  serial.getCurrentWarehouseId(),
                  serial.getCurrentCustomerId(),
                  serial.getWarrantyStartDate(),
                  serial.getWarrantyEndDate()
          ),
          batch == null ? null : new ProductScanResponse.BatchMatch(
                  batch.getId(),
                  batch.getBatchNumber(),
                  batch.getManufacturerBatchNumber(),
                  batch.getStatus(),
                  batch.getManufacturedOn(),
                  batch.getExpiryOn()
          ),
          stock
  );
 }

 private ProductScanResponse.StockSnapshot stockSnapshot(Long organizationId, Long productId, Long warehouseId, Long batchId) {
  List<InventoryBalance> balances = warehouseId == null
          ? inventoryBalanceRepository.findByOrganizationIdAndProductId(organizationId, productId)
          : inventoryBalanceRepository.findByOrganizationIdAndProductIdAndWarehouseId(organizationId, productId, warehouseId);

  BigDecimal onHand = BigDecimal.ZERO;
  BigDecimal reserved = BigDecimal.ZERO;
  BigDecimal available = BigDecimal.ZERO;

  for (InventoryBalance balance : balances) {
   if (batchId != null && !batchId.equals(balance.getBatchId())) {
    continue;
   }
   onHand = onHand.add(zeroIfNull(balance.getOnHandBaseQuantity()));
   reserved = reserved.add(zeroIfNull(balance.getReservedBaseQuantity()));
   available = available.add(zeroIfNull(balance.getAvailableBaseQuantity()));
  }

  return new ProductScanResponse.StockSnapshot(warehouseId, onHand, reserved, available);
 }

 private BigDecimal zeroIfNull(BigDecimal value) {
  return value == null ? BigDecimal.ZERO : value;
 }

 private Boolean booleanValue(Boolean value) {
  return Boolean.TRUE.equals(value);
 }

 private String trimToNull(String value) {
  if (value == null) {
   return null;
  }
  String trimmed = value.trim();
  return trimmed.isEmpty() ? null : trimmed;
 }

 private String fallbackSku(Product product) {
  return "PRD-" + product.getId();
 }
}
