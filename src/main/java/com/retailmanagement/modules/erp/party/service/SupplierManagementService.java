package com.retailmanagement.modules.erp.party.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.party.dto.SupplierDtos;
import com.retailmanagement.modules.erp.party.entity.StoreProductSupplierPreference;
import com.retailmanagement.modules.erp.party.entity.StoreSupplierTerms;
import com.retailmanagement.modules.erp.party.entity.Supplier;
import com.retailmanagement.modules.erp.party.entity.SupplierProduct;
import com.retailmanagement.modules.erp.party.repository.StoreProductSupplierPreferenceRepository;
import com.retailmanagement.modules.erp.party.repository.StoreSupplierTermsRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierProductRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierManagementService {

    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final StoreSupplierTermsRepository storeSupplierTermsRepository;
    private final StoreProductSupplierPreferenceRepository storeProductSupplierPreferenceRepository;
    private final StoreProductRepository storeProductRepository;
    private final ErpAccessGuard accessGuard;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<SupplierDtos.SupplierResponse> listSuppliers(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        return supplierRepository.findByOrganizationId(organizationId).stream()
                .sorted(Comparator.comparing(Supplier::getSupplierCode))
                .map(this::toSupplierResponse)
                .toList();
    }

    public SupplierDtos.SupplierResponse createSupplier(Long organizationId, Long branchId, SupplierDtos.UpsertSupplierRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        Supplier supplier = new Supplier();
        supplier.setOrganizationId(organizationId);
        supplier.setBranchId(branchId);
        applySupplierRequest(supplier, request);
        return toSupplierResponse(supplierRepository.save(supplier));
    }

    public SupplierDtos.SupplierResponse updateSupplier(Long organizationId, Long supplierId, SupplierDtos.UpsertSupplierRequest request) {
        accessGuard.assertOrganizationAccess(organizationId);
        Supplier supplier = supplierRepository.findByIdAndOrganizationId(supplierId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
        applySupplierRequest(supplier, request);
        return toSupplierResponse(supplierRepository.save(supplier));
    }

    @Transactional(readOnly = true)
    public SupplierDtos.SupplierCatalogResponse supplierCatalog(Long organizationId, Long supplierId) {
        accessGuard.assertOrganizationAccess(organizationId);
        Supplier supplier = supplierRepository.findByIdAndOrganizationId(supplierId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
        StoreSupplierTerms terms = storeSupplierTermsRepository.findByOrganizationIdAndSupplierId(organizationId, supplierId).orElse(null);

        List<StoreProduct> storeProducts = storeProductRepository.findByOrganizationId(organizationId);
        Map<Long, StoreProduct> storeProductsByMasterId = storeProducts.stream()
                .collect(Collectors.toMap(StoreProduct::getProductId, Function.identity(), (left, right) -> left));

        List<SupplierDtos.PurchasableStoreProductResponse> products = supplierProductRepository
                .findByOrganizationIdAndSupplierIdAndIsActiveTrueOrderByIsPreferredDescPriorityAscIdAsc(organizationId, supplierId)
                .stream()
                .map(supplierProduct -> {
                    StoreProduct storeProduct = storeProductsByMasterId.get(supplierProduct.getProductId());
                    if (storeProduct == null) {
                        return null;
                    }
                    return new SupplierDtos.PurchasableStoreProductResponse(
                            storeProduct.getId(),
                            storeProduct.getProductId(),
                            supplierProduct.getId(),
                            storeProduct.getSku(),
                            storeProduct.getName(),
                            supplierProduct.getSupplierProductCode(),
                            supplierProduct.getSupplierProductName(),
                            supplierProduct.getIsPreferred(),
                            supplierProduct.getPriority()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        return new SupplierDtos.SupplierCatalogResponse(
                toSupplierResponse(supplier),
                terms == null ? null : toTermsResponse(terms),
                products
        );
    }

    public SupplierDtos.SupplierProductResponse upsertSupplierProduct(
            Long organizationId,
            Long supplierId,
            Long supplierProductId,
            SupplierDtos.UpsertSupplierProductRequest request
    ) {
        accessGuard.assertOrganizationAccess(organizationId);
        supplierRepository.findByIdAndOrganizationId(supplierId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));

        SupplierProduct supplierProduct = supplierProductId == null
                ? new SupplierProduct()
                : supplierProductRepository.findByIdAndOrganizationId(supplierProductId, organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier product not found: " + supplierProductId));

        if (supplierProductId != null && !supplierId.equals(supplierProduct.getSupplierId())) {
            throw new BusinessException("Supplier product does not belong to supplier " + supplierId);
        }

        supplierProduct.setOrganizationId(organizationId);
        supplierProduct.setSupplierId(supplierId);
        supplierProduct.setProductId(request.productId());
        supplierProduct.setSupplierProductCode(trimToNull(request.supplierProductCode()));
        supplierProduct.setSupplierProductName(trimToNull(request.supplierProductName()));
        supplierProduct.setPriority(request.priority() == null ? 1 : request.priority());
        supplierProduct.setIsPreferred(Boolean.TRUE.equals(request.isPreferred()));
        supplierProduct.setIsActive(request.isActive() == null || Boolean.TRUE.equals(request.isActive()));
        return toSupplierProductResponse(supplierProductRepository.save(supplierProduct));
    }

    @Transactional(readOnly = true)
    public List<SupplierDtos.SupplierProductResponse> listSupplierProducts(Long organizationId, Long supplierId) {
        accessGuard.assertOrganizationAccess(organizationId);
        supplierRepository.findByIdAndOrganizationId(supplierId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
        return supplierProductRepository.findByOrganizationIdAndSupplierIdAndIsActiveTrueOrderByIsPreferredDescPriorityAscIdAsc(organizationId, supplierId)
                .stream()
                .map(this::toSupplierProductResponse)
                .toList();
    }

    public SupplierDtos.StoreSupplierTermsResponse upsertStoreSupplierTerms(
            Long organizationId,
            Long supplierId,
            SupplierDtos.UpsertStoreSupplierTermsRequest request
    ) {
        accessGuard.assertOrganizationAccess(organizationId);
        supplierRepository.findByIdAndOrganizationId(supplierId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));

        StoreSupplierTerms terms = storeSupplierTermsRepository.findByOrganizationIdAndSupplierId(organizationId, supplierId)
                .orElseGet(StoreSupplierTerms::new);
        terms.setOrganizationId(organizationId);
        terms.setSupplierId(supplierId);
        terms.setPaymentTerms(trimToNull(request.paymentTerms()));
        terms.setCreditLimit(request.creditLimit() == null ? BigDecimal.ZERO : request.creditLimit());
        terms.setCreditDays(request.creditDays());
        terms.setIsPreferred(Boolean.TRUE.equals(request.isPreferred()));
        terms.setIsActive(request.isActive() == null || Boolean.TRUE.equals(request.isActive()));
        terms.setContractStart(request.contractStart());
        terms.setContractEnd(request.contractEnd());
        terms.setOrderViaEmail(Boolean.TRUE.equals(request.orderViaEmail()));
        terms.setOrderViaWhatsapp(Boolean.TRUE.equals(request.orderViaWhatsapp()));
        terms.setRemarks(trimToNull(request.remarks()));
        return toTermsResponse(storeSupplierTermsRepository.save(terms));
    }

    @Transactional(readOnly = true)
    public SupplierDtos.StoreSupplierTermsResponse getStoreSupplierTerms(Long organizationId, Long supplierId) {
        accessGuard.assertOrganizationAccess(organizationId);
        return storeSupplierTermsRepository.findByOrganizationIdAndSupplierId(organizationId, supplierId)
                .map(this::toTermsResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public SupplierDtos.StoreProductSupplierPreferenceResponse getStoreProductSupplierPreference(Long organizationId, Long storeProductId) {
        accessGuard.assertOrganizationAccess(organizationId);
        return storeProductSupplierPreferenceRepository.findByOrganizationIdAndStoreProductId(organizationId, storeProductId)
                .map(this::toStoreProductSupplierPreferenceResponse)
                .orElse(null);
    }

    public SupplierDtos.StoreProductSupplierPreferenceResponse upsertStoreProductSupplierPreference(
            Long organizationId,
            Long storeProductId,
            SupplierDtos.UpsertStoreProductSupplierPreferenceRequest request
    ) {
        accessGuard.assertOrganizationAccess(organizationId);
        StoreProduct storeProduct = storeProductRepository.findById(storeProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Store product not found: " + storeProductId));
        if (!organizationId.equals(storeProduct.getOrganizationId())) {
            throw new BusinessException("Store product does not belong to organization " + organizationId + ": " + storeProductId);
        }
        Supplier supplier = supplierRepository.findByIdAndOrganizationId(request.supplierId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));
        SupplierProduct supplierProduct = supplierProductRepository.findByIdAndOrganizationId(request.supplierProductId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier product not found: " + request.supplierProductId()));
        if (!supplier.getId().equals(supplierProduct.getSupplierId())) {
            throw new BusinessException("Supplier product does not belong to supplier " + supplier.getId());
        }
        if (!storeProduct.getProductId().equals(supplierProduct.getProductId())) {
            throw new BusinessException("Supplier product does not match store product master for store product " + storeProductId);
        }
        StoreProductSupplierPreference preference = storeProductSupplierPreferenceRepository
                .findByOrganizationIdAndStoreProductId(organizationId, storeProductId)
                .orElseGet(StoreProductSupplierPreference::new);
        preference.setOrganizationId(organizationId);
        preference.setStoreProductId(storeProductId);
        preference.setSupplierId(supplier.getId());
        preference.setSupplierProductId(supplierProduct.getId());
        preference.setIsActive(request.isActive() == null || Boolean.TRUE.equals(request.isActive()));
        preference.setRemarks(trimToNull(request.remarks()));
        return toStoreProductSupplierPreferenceResponse(storeProductSupplierPreferenceRepository.save(preference));
    }

    @Transactional(readOnly = true)
    public SupplierDtos.StoreProductSuppliersResponse getStoreProductSuppliers(Long organizationId, Long storeProductId) {
        accessGuard.assertOrganizationAccess(organizationId);
        StoreProduct storeProduct = storeProductRepository.findById(storeProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Store product not found: " + storeProductId));
        if (!organizationId.equals(storeProduct.getOrganizationId())) {
            throw new BusinessException("Store product does not belong to organization " + organizationId + ": " + storeProductId);
        }

        List<SupplierProduct> supplierProducts = supplierProductRepository
                .findByOrganizationIdAndProductIdAndIsActiveTrueOrderByIsPreferredDescPriorityAscIdAsc(organizationId, storeProduct.getProductId());
        Map<Long, Supplier> suppliersById = supplierRepository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(Supplier::getId, Function.identity(), (left, right) -> left));
        StoreProductSupplierPreference preference = storeProductSupplierPreferenceRepository
                .findByOrganizationIdAndStoreProductIdAndIsActiveTrue(organizationId, storeProductId)
                .orElse(null);

        List<SupplierDtos.StoreProductSupplierLinkResponse> links = supplierProducts.stream()
                .map(supplierProduct -> {
                    Supplier supplier = suppliersById.get(supplierProduct.getSupplierId());
                    return new SupplierDtos.StoreProductSupplierLinkResponse(
                            supplierProduct.getSupplierId(),
                            supplier != null ? supplier.getSupplierCode() : null,
                            supplier != null ? supplier.getName() : null,
                            supplierProduct.getId(),
                            supplierProduct.getSupplierProductCode(),
                            supplierProduct.getSupplierProductName(),
                            supplierProduct.getPriority(),
                            supplierProduct.getIsPreferred(),
                            preference != null && supplierProduct.getId().equals(preference.getSupplierProductId()),
                            supplierProduct.getIsActive()
                    );
                })
                .toList();

        return new SupplierDtos.StoreProductSuppliersResponse(
                storeProductId,
                storeProduct.getProductId(),
                preference != null ? preference.getSupplierId() : null,
                preference != null ? preference.getSupplierProductId() : null,
                links
        );
    }

    public SupplierDtos.StoreProductSuppliersResponse upsertStoreProductSuppliers(
            Long organizationId,
            Long storeProductId,
            SupplierDtos.UpsertStoreProductSuppliersRequest request
    ) {
        accessGuard.assertOrganizationAccess(organizationId);
        StoreProduct storeProduct = storeProductRepository.findById(storeProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Store product not found: " + storeProductId));
        if (!organizationId.equals(storeProduct.getOrganizationId())) {
            throw new BusinessException("Store product does not belong to organization " + organizationId + ": " + storeProductId);
        }
        if (request.supplierLinks() == null || request.supplierLinks().isEmpty()) {
            throw new BusinessException("At least one supplier link is required");
        }

        List<SupplierProduct> savedSupplierProducts = new java.util.ArrayList<>();
        for (SupplierDtos.UpsertStoreProductSupplierLinkRequest link : request.supplierLinks()) {
            Supplier supplier = supplierRepository.findByIdAndOrganizationId(link.supplierId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + link.supplierId()));
            SupplierProduct supplierProduct = resolveSupplierProductForStoreProduct(organizationId, storeProduct, supplier, link);
            supplierProduct.setOrganizationId(organizationId);
            supplierProduct.setSupplierId(supplier.getId());
            supplierProduct.setProductId(storeProduct.getProductId());
            supplierProduct.setSupplierProductCode(trimToNull(link.supplierProductCode()));
            supplierProduct.setSupplierProductName(trimToNull(link.supplierProductName()));
            supplierProduct.setPriority(link.priority() == null ? 1 : link.priority());
            supplierProduct.setIsPreferred(Boolean.TRUE.equals(link.isPreferred()));
            supplierProduct.setIsActive(link.isActive() == null || Boolean.TRUE.equals(link.isActive()));
            savedSupplierProducts.add(supplierProductRepository.save(supplierProduct));
        }

        SupplierProduct preferred = resolvePreferredSupplierProduct(organizationId, storeProduct, request, savedSupplierProducts);
        StoreProductSupplierPreference preference = storeProductSupplierPreferenceRepository
                .findByOrganizationIdAndStoreProductId(organizationId, storeProductId)
                .orElseGet(StoreProductSupplierPreference::new);
        if (preferred == null) {
            if (preference.getId() != null) {
                preference.setOrganizationId(organizationId);
                preference.setStoreProductId(storeProductId);
                preference.setIsActive(false);
                preference.setRemarks(trimToNull(request.preferredRemarks()));
                storeProductSupplierPreferenceRepository.save(preference);
            }
            return getStoreProductSuppliers(organizationId, storeProductId);
        }

        preference.setOrganizationId(organizationId);
        preference.setStoreProductId(storeProductId);
        preference.setSupplierId(preferred.getSupplierId());
        preference.setSupplierProductId(preferred.getId());
        preference.setIsActive(request.preferredIsActive() == null || Boolean.TRUE.equals(request.preferredIsActive()));
        preference.setRemarks(trimToNull(request.preferredRemarks()));
        storeProductSupplierPreferenceRepository.save(preference);
        return getStoreProductSuppliers(organizationId, storeProductId);
    }

    private void applySupplierRequest(Supplier supplier, SupplierDtos.UpsertSupplierRequest request) {
        supplier.setSupplierCode(resolveSupplierCode(supplier, request.supplierCode()));
        supplier.setName(request.name().trim());
        supplier.setLegalName(trimToNull(request.legalName()) == null ? request.name().trim() : request.legalName().trim());
        supplier.setTradeName(trimToNull(request.tradeName()) == null ? supplier.getName() : request.tradeName().trim());
        supplier.setPhone(trimToNull(request.phone()));
        supplier.setEmail(trimToNull(request.email()));
        supplier.setGstin(trimToNull(request.gstin()));
        supplier.setLinkedOrganizationId(request.linkedOrganizationId());
        supplier.setBillingAddress(trimToNull(request.billingAddress()));
        supplier.setShippingAddress(trimToNull(request.shippingAddress()));
        supplier.setState(trimToNull(request.state()));
        supplier.setStateCode(trimToNull(request.stateCode()));
        supplier.setContactPersonName(trimToNull(request.contactPersonName()));
        supplier.setContactPersonPhone(trimToNull(request.contactPersonPhone()));
        supplier.setContactPersonEmail(trimToNull(request.contactPersonEmail()));
        supplier.setPaymentTerms(trimToNull(request.paymentTerms()));
        supplier.setIsPlatformLinked(Boolean.TRUE.equals(request.isPlatformLinked()) || request.linkedOrganizationId() != null);
        supplier.setNotes(trimToNull(request.notes()));
        supplier.setStatus(trimToNull(request.status()) == null ? "ACTIVE" : request.status().trim().toUpperCase());
    }

    private String resolveSupplierCode(Supplier supplier, String requestedCode) {
        String normalized = trimToNull(requestedCode);
        if (normalized != null) {
            String code = normalized.toUpperCase();
            boolean exists = supplier.getId() == null
                    ? supplierRepository.existsByOrganizationIdAndSupplierCode(supplier.getOrganizationId(), code)
                    : supplierRepository.existsByOrganizationIdAndSupplierCodeAndIdNot(supplier.getOrganizationId(), code, supplier.getId());
            if (exists) {
                throw new BusinessException("Supplier code already exists: " + code);
            }
            return code;
        }
        if (trimToNull(supplier.getSupplierCode()) != null) {
            return supplier.getSupplierCode().trim().toUpperCase();
        }
        return generateSupplierCode(supplier.getOrganizationId());
    }

    private String generateSupplierCode(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        String orgCode = organization.getCode().trim().toUpperCase();
        for (int sequence = 1; sequence < 100000; sequence++) {
            String generated = "SUP-" + orgCode + "-" + String.format("%04d", sequence);
            if (!supplierRepository.existsByOrganizationIdAndSupplierCode(organizationId, generated)) {
                return generated;
            }
        }
        throw new BusinessException("Unable to generate supplier code for organization " + organizationId);
    }

    private SupplierDtos.SupplierResponse toSupplierResponse(Supplier supplier) {
        return new SupplierDtos.SupplierResponse(
                supplier.getId(),
                supplier.getOrganizationId(),
                supplier.getBranchId(),
                supplier.getLinkedOrganizationId(),
                supplier.getSupplierCode(),
                supplier.getName(),
                supplier.getLegalName(),
                supplier.getTradeName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getGstin(),
                supplier.getBillingAddress(),
                supplier.getShippingAddress(),
                supplier.getState(),
                supplier.getStateCode(),
                supplier.getContactPersonName(),
                supplier.getContactPersonPhone(),
                supplier.getContactPersonEmail(),
                supplier.getPaymentTerms(),
                supplier.getIsPlatformLinked(),
                supplier.getNotes(),
                supplier.getStatus(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }

    private SupplierDtos.SupplierProductResponse toSupplierProductResponse(SupplierProduct supplierProduct) {
        return new SupplierDtos.SupplierProductResponse(
                supplierProduct.getId(),
                supplierProduct.getOrganizationId(),
                supplierProduct.getSupplierId(),
                supplierProduct.getProductId(),
                supplierProduct.getSupplierProductCode(),
                supplierProduct.getSupplierProductName(),
                supplierProduct.getPriority(),
                supplierProduct.getIsPreferred(),
                supplierProduct.getIsActive(),
                supplierProduct.getCreatedAt(),
                supplierProduct.getUpdatedAt()
        );
    }

    private SupplierDtos.StoreSupplierTermsResponse toTermsResponse(StoreSupplierTerms terms) {
        return new SupplierDtos.StoreSupplierTermsResponse(
                terms.getId(),
                terms.getOrganizationId(),
                terms.getSupplierId(),
                terms.getPaymentTerms(),
                terms.getCreditLimit(),
                terms.getCreditDays(),
                terms.getIsPreferred(),
                terms.getIsActive(),
                terms.getContractStart(),
                terms.getContractEnd(),
                terms.getOrderViaEmail(),
                terms.getOrderViaWhatsapp(),
                terms.getRemarks(),
                terms.getCreatedAt(),
                terms.getUpdatedAt()
        );
    }

    private SupplierDtos.StoreProductSupplierPreferenceResponse toStoreProductSupplierPreferenceResponse(StoreProductSupplierPreference preference) {
        return new SupplierDtos.StoreProductSupplierPreferenceResponse(
                preference.getId(),
                preference.getOrganizationId(),
                preference.getStoreProductId(),
                preference.getSupplierId(),
                preference.getSupplierProductId(),
                preference.getIsActive(),
                preference.getRemarks(),
                preference.getCreatedAt(),
                preference.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private SupplierProduct resolveSupplierProductForStoreProduct(
            Long organizationId,
            StoreProduct storeProduct,
            Supplier supplier,
            SupplierDtos.UpsertStoreProductSupplierLinkRequest request
    ) {
        if (request.supplierProductId() == null) {
            return new SupplierProduct();
        }
        SupplierProduct supplierProduct = supplierProductRepository.findByIdAndOrganizationId(request.supplierProductId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier product not found: " + request.supplierProductId()));
        if (!supplier.getId().equals(supplierProduct.getSupplierId())) {
            throw new BusinessException("Supplier product does not belong to supplier " + supplier.getId());
        }
        if (!storeProduct.getProductId().equals(supplierProduct.getProductId())) {
            throw new BusinessException("Supplier product does not match store product master for store product " + storeProduct.getId());
        }
        return supplierProduct;
    }

    private SupplierProduct resolvePreferredSupplierProduct(
            Long organizationId,
            StoreProduct storeProduct,
            SupplierDtos.UpsertStoreProductSuppliersRequest request,
            List<SupplierProduct> savedSupplierProducts
    ) {
        if (request.preferredSupplierProductId() != null) {
            SupplierProduct preferred = supplierProductRepository.findByIdAndOrganizationId(request.preferredSupplierProductId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier product not found: " + request.preferredSupplierProductId()));
            if (!storeProduct.getProductId().equals(preferred.getProductId())) {
                throw new BusinessException("Preferred supplier product does not match store product master for store product " + storeProduct.getId());
            }
            if (request.preferredSupplierId() != null && !request.preferredSupplierId().equals(preferred.getSupplierId())) {
                throw new BusinessException("Preferred supplier and supplier product do not match");
            }
            return preferred;
        }

        if (request.preferredSupplierId() != null) {
            return savedSupplierProducts.stream()
                    .filter(link -> request.preferredSupplierId().equals(link.getSupplierId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Preferred supplier must be included in supplier links"));
        }

        List<SupplierProduct> preferredLinks = savedSupplierProducts.stream()
                .filter(link -> Boolean.TRUE.equals(link.getIsPreferred()))
                .toList();
        if (preferredLinks.size() > 1) {
            throw new BusinessException("Only one preferred supplier link can be marked for a store product");
        }
        return preferredLinks.isEmpty() ? null : preferredLinks.getFirst();
    }
}
