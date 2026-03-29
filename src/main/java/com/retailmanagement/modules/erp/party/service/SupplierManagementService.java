package com.retailmanagement.modules.erp.party.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.party.dto.SupplierDtos;
import com.retailmanagement.modules.erp.party.entity.StoreSupplierTerms;
import com.retailmanagement.modules.erp.party.entity.Supplier;
import com.retailmanagement.modules.erp.party.entity.SupplierProduct;
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
    private final StoreProductRepository storeProductRepository;
    private final ErpAccessGuard accessGuard;

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

    private void applySupplierRequest(Supplier supplier, SupplierDtos.UpsertSupplierRequest request) {
        supplier.setSupplierCode(request.supplierCode().trim());
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

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
