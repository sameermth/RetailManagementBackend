package com.retailmanagement.modules.erp.catalog.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.dto.ProductDtos;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.entity.StoreProductPrice;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductPriceRepository;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.party.entity.StoreCustomerTerms;
import com.retailmanagement.modules.erp.party.repository.StoreCustomerTermsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreProductPricingService {

    private final StoreProductRepository storeProductRepository;
    private final StoreProductPriceRepository storeProductPriceRepository;
    private final StoreCustomerTermsRepository storeCustomerTermsRepository;
    private final ErpAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public List<ProductDtos.StoreProductPriceResponse> listPrices(Long organizationId, Long storeProductId) {
        requireStoreProduct(organizationId, storeProductId);
        return storeProductPriceRepository.findByOrganizationIdAndStoreProductIdOrderByEffectiveFromDescIdDesc(organizationId, storeProductId)
                .stream()
                .map(this::toPriceResponse)
                .toList();
    }

    public ProductDtos.StoreProductPriceResponse createPrice(Long organizationId, Long storeProductId, ProductDtos.UpsertStoreProductPriceRequest request) {
        requireStoreProduct(organizationId, storeProductId);
        StoreProductPrice price = new StoreProductPrice();
        price.setOrganizationId(organizationId);
        price.setStoreProductId(storeProductId);
        applyPriceRequest(price, request);
        return toPriceResponse(storeProductPriceRepository.save(price));
    }

    public ProductDtos.StoreProductPriceResponse updatePrice(Long organizationId,
                                                             Long storeProductId,
                                                             Long storeProductPriceId,
                                                             ProductDtos.UpsertStoreProductPriceRequest request) {
        requireStoreProduct(organizationId, storeProductId);
        StoreProductPrice price = storeProductPriceRepository.findByIdAndOrganizationId(storeProductPriceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Store product price not found: " + storeProductPriceId));
        if (!Objects.equals(price.getStoreProductId(), storeProductId)) {
            throw new BusinessException("Price " + storeProductPriceId + " does not belong to store product " + storeProductId);
        }
        applyPriceRequest(price, request);
        return toPriceResponse(storeProductPriceRepository.save(price));
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveUnitPrice(Long organizationId,
                                       Long storeProductId,
                                       Long customerId,
                                       BigDecimal baseQuantity,
                                       LocalDate effectiveDate) {
        StoreProduct storeProduct = requireStoreProduct(organizationId, storeProductId);
        LocalDate pricingDate = effectiveDate == null ? LocalDate.now() : effectiveDate;
        BigDecimal quantity = baseQuantity == null ? BigDecimal.ZERO : baseQuantity;

        StoreCustomerTerms customerTerms = customerId == null
                ? null
                : storeCustomerTermsRepository.findByOrganizationIdAndCustomerId(organizationId, customerId).orElse(null);
        String preferredTier = normalize(customerTerms == null ? null : customerTerms.getPriceTier());
        String customerSegment = normalize(customerTerms == null ? "RETAIL" : customerTerms.getCustomerSegment());

        List<StoreProductPrice> matches = storeProductPriceRepository
                .findByOrganizationIdAndStoreProductIdOrderByEffectiveFromDescIdDesc(organizationId, storeProductId)
                .stream()
                .filter(price -> Boolean.TRUE.equals(price.getIsActive()))
                .filter(price -> "SELLING".equalsIgnoreCase(price.getPriceType()))
                .filter(price -> !pricingDate.isBefore(price.getEffectiveFrom()))
                .filter(price -> price.getEffectiveTo() == null || !pricingDate.isAfter(price.getEffectiveTo()))
                .filter(price -> price.getMinQuantity() == null || quantity.compareTo(price.getMinQuantity()) >= 0)
                .sorted(Comparator
                        .comparing((StoreProductPrice price) -> segmentPriority(price, preferredTier, customerSegment))
                        .thenComparing((StoreProductPrice price) -> Boolean.TRUE.equals(price.getIsDefault()) ? 0 : 1)
                        .thenComparing(StoreProductPrice::getMinQuantity, Comparator.nullsFirst(Comparator.reverseOrder()))
                        .thenComparing(StoreProductPrice::getEffectiveFrom, Comparator.reverseOrder()))
                .toList();

        if (!matches.isEmpty()) {
            return matches.get(0).getPrice();
        }
        if (storeProduct.getDefaultSalePrice() != null) {
            return storeProduct.getDefaultSalePrice();
        }
        throw new BusinessException("No active selling price configured for product " + storeProduct.getSku());
    }

    private void applyPriceRequest(StoreProductPrice price, ProductDtos.UpsertStoreProductPriceRequest request) {
        price.setPriceType(normalize(request.priceType()) == null ? "SELLING" : normalize(request.priceType()));
        price.setCustomerSegment(normalize(request.customerSegment()));
        price.setPrice(request.price());
        price.setMinQuantity(request.minQuantity());
        price.setEffectiveFrom(request.effectiveFrom() == null ? LocalDate.now() : request.effectiveFrom());
        price.setEffectiveTo(request.effectiveTo());
        price.setIsDefault(request.isDefault() != null && request.isDefault());
        price.setIsActive(request.isActive() == null || request.isActive());
    }

    private int segmentPriority(StoreProductPrice price, String preferredTier, String customerSegment) {
        String priceSegment = normalize(price.getCustomerSegment());
        if (preferredTier != null && preferredTier.equals(priceSegment)) {
            return 0;
        }
        if (customerSegment != null && customerSegment.equals(priceSegment)) {
            return 1;
        }
        if ("RETAIL".equals(priceSegment)) {
            return 2;
        }
        if (priceSegment == null) {
            return 3;
        }
        return 4;
    }

    private StoreProduct requireStoreProduct(Long organizationId, Long storeProductId) {
        accessGuard.assertOrganizationAccess(organizationId);
        StoreProduct storeProduct = storeProductRepository.findById(storeProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Store product not found: " + storeProductId));
        if (!Objects.equals(storeProduct.getOrganizationId(), organizationId)) {
            throw new BusinessException("Store product " + storeProductId + " does not belong to organization " + organizationId);
        }
        return storeProduct;
    }

    private ProductDtos.StoreProductPriceResponse toPriceResponse(StoreProductPrice price) {
        return new ProductDtos.StoreProductPriceResponse(
                price.getId(),
                price.getOrganizationId(),
                price.getStoreProductId(),
                price.getPriceType(),
                price.getCustomerSegment(),
                price.getPrice(),
                price.getMinQuantity(),
                price.getEffectiveFrom(),
                price.getEffectiveTo(),
                price.getIsDefault(),
                price.getIsActive(),
                price.getCreatedAt(),
                price.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
