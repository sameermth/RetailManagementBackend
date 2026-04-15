package com.retailmanagement.modules.erp.inventory.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
import com.retailmanagement.modules.erp.foundation.entity.Warehouse;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import com.retailmanagement.modules.erp.foundation.repository.WarehouseRepository;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.party.entity.StoreProductSupplierPreference;
import com.retailmanagement.modules.erp.party.entity.StoreSupplierTerms;
import com.retailmanagement.modules.erp.party.entity.Supplier;
import com.retailmanagement.modules.erp.party.entity.SupplierProduct;
import com.retailmanagement.modules.erp.party.repository.StoreProductSupplierPreferenceRepository;
import com.retailmanagement.modules.erp.party.repository.StoreSupplierTermsRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierProductRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrder;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrderLine;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseOrderLineRepository;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseOrderRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryPlanningService {

    private final StoreProductRepository storeProductRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final WarehouseRepository warehouseRepository;
    private final BranchRepository branchRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final StoreSupplierTermsRepository storeSupplierTermsRepository;
    private final StoreProductSupplierPreferenceRepository storeProductSupplierPreferenceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final InventoryOperationsService inventoryOperationsService;
    private final ErpAccessGuard accessGuard;
    private final SubscriptionAccessService subscriptionAccessService;

    @Transactional(readOnly = true)
    public List<InventoryDtos.InventoryReplenishmentRecommendationResponse> listRecommendations(
            Long organizationId,
            Long branchId,
            Long warehouseId,
            boolean actionableOnly
    ) {
        accessGuard.assertOrganizationAccess(organizationId);
        if (branchId != null) {
            accessGuard.assertBranchAccess(organizationId, branchId);
        }
        subscriptionAccessService.assertFeature(organizationId, "inventory");

        List<Warehouse> targetWarehouses = warehousesForScope(organizationId, branchId, warehouseId);
        if (targetWarehouses.isEmpty()) {
            return List.of();
        }
        List<Warehouse> organizationWarehouses = warehouseRepository.findByOrganizationIdOrderByBranchIdAscIdAsc(organizationId);

        Map<Long, Map<Long, BigDecimal>> availableByWarehouseAndProduct = aggregateAvailability(
                inventoryBalanceRepository.findByOrganizationId(organizationId)
        );

        List<InventoryDtos.InventoryReplenishmentRecommendationResponse> recommendations = new ArrayList<>();
        for (StoreProduct product : storeProductRepository.findByOrganizationId(organizationId)) {
            if (Boolean.FALSE.equals(product.getIsActive()) || Boolean.TRUE.equals(product.getIsServiceItem())) {
                continue;
            }
            for (Warehouse warehouse : targetWarehouses) {
                BigDecimal available = availableByWarehouseAndProduct
                        .getOrDefault(warehouse.getId(), Map.of())
                        .getOrDefault(product.getId(), BigDecimal.ZERO);
                BigDecimal reorderLevel = safe(product.getReorderLevelBaseQty());
                BigDecimal minStock = safe(product.getMinStockBaseQty());
                BigDecimal recommended = recommendedReorderQuantity(available, reorderLevel, minStock);
                boolean actionRequired = available.compareTo(reorderLevel) <= 0;
                if (actionableOnly && !actionRequired) {
                    continue;
                }

                Optional<TransferCandidate> transferCandidate = isWarehouseTransferEligible(product)
                        ? resolveTransferCandidate(
                                organizationWarehouses,
                                availableByWarehouseAndProduct,
                                warehouse.getId(),
                                product,
                                recommended
                        )
                        : Optional.empty();
                Optional<SupplierCandidate> supplierCandidate = resolveSupplierCandidate(organizationId, product, LocalDate.now());

                BigDecimal transferQty = transferCandidate.map(TransferCandidate::recommendedTransferQuantity).orElse(BigDecimal.ZERO);
                BigDecimal purchaseQty = recommended.subtract(transferQty).max(BigDecimal.ZERO);

                recommendations.add(new InventoryDtos.InventoryReplenishmentRecommendationResponse(
                        product.getId(),
                        product.getProductId(),
                        product.getSku(),
                        product.getName(),
                        warehouse.getBranchId(),
                        warehouse.getId(),
                        warehouse.getCode(),
                        warehouse.getName(),
                        available,
                        minStock,
                        reorderLevel,
                        recommended,
                        transferCandidate.map(candidate -> new InventoryDtos.InternalTransferSuggestion(
                                candidate.sourceWarehouse().getId(),
                                candidate.sourceWarehouse().getCode(),
                                candidate.sourceWarehouse().getName(),
                                candidate.sourceAvailable(),
                                candidate.sourceExcess(),
                                candidate.recommendedTransferQuantity()
                        )).orElse(null),
                        supplierCandidate.map(candidate -> new InventoryDtos.PurchaseSuggestion(
                                candidate.supplier().getId(),
                                candidate.supplier().getSupplierCode(),
                                candidate.supplier().getName(),
                                candidate.supplierProduct().getId(),
                                purchaseQty,
                                hasOpenPurchaseOrderForProduct(organizationId, candidate.supplier().getId(), product.getId())
                        )).orElse(null),
                        actionRequired
                ));
            }
        }

        return recommendations.stream()
                .sorted(Comparator
                        .comparing(InventoryDtos.InventoryReplenishmentRecommendationResponse::actionRequired).reversed()
                        .thenComparing(InventoryDtos.InventoryReplenishmentRecommendationResponse::warehouseName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(InventoryDtos.InventoryReplenishmentRecommendationResponse::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public InventoryDtos.DraftPurchaseOrderSummaryResponse createDraftPurchaseOrder(
            InventoryDtos.CreateReplenishmentPurchaseOrderRequest request
    ) {
        accessGuard.assertOrganizationAccess(request.organizationId());
        subscriptionAccessService.assertFeature(request.organizationId(), "inventory");
        subscriptionAccessService.assertFeature(request.organizationId(), "purchases");

        StoreProduct product = storeProductRepository.findById(request.storeProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Store product not found: " + request.storeProductId()));
        if (!request.organizationId().equals(product.getOrganizationId())) {
            throw new BusinessException("Store product does not belong to organization " + request.organizationId());
        }

        Warehouse targetWarehouse = resolveWarehouseForPurchase(request.organizationId(), request.branchId(), request.warehouseId());
        SupplierCandidate supplierCandidate = request.supplierId() != null
                ? resolveExplicitSupplier(request.organizationId(), product, request.supplierId(), LocalDate.now())
                : resolveSupplierCandidate(request.organizationId(), product, LocalDate.now())
                        .orElseThrow(() -> new BusinessException("No eligible supplier mapping found for product " + product.getSku()));

        if (hasOpenPurchaseOrderForProduct(request.organizationId(), supplierCandidate.supplier().getId(), product.getId())) {
            throw new BusinessException("An open purchase order already exists for product " + product.getSku());
        }

        BigDecimal available = availableForWarehouse(request.organizationId(), targetWarehouse.getId(), product.getId());
        BigDecimal recommended = recommendedReorderQuantity(available, safe(product.getReorderLevelBaseQty()), safe(product.getMinStockBaseQty()));
        BigDecimal quantity = safe(request.quantity());
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            quantity = recommended;
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Recommended quantity is zero for product " + product.getSku());
        }

        String poNumber = "PLAN-PO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
        PurchaseOrder order = new PurchaseOrder();
        order.setOrganizationId(request.organizationId());
        order.setBranchId(targetWarehouse.getBranchId());
        order.setSupplierId(supplierCandidate.supplier().getId());
        order.setPoNumber(poNumber);
        order.setPoDate(LocalDate.now());
        order.setSupplierGstin(supplierCandidate.supplier().getGstin());
        order.setStatus(ErpDocumentStatuses.DRAFT);
        order.setRemarks(request.remarks() == null || request.remarks().isBlank()
                ? "Created from inventory planning for warehouse " + targetWarehouse.getCode()
                : request.remarks());
        order = purchaseOrderRepository.save(order);

        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setPurchaseOrderId(order.getId());
        line.setProductId(product.getId());
        line.setSupplierProductId(supplierCandidate.supplierProduct().getId());
        line.setProductMasterId(product.getProductId());
        line.setUomId(product.getBaseUomId());
        line.setSkuSnapshot(product.getSku());
        line.setProductNameSnapshot(product.getName());
        line.setSupplierProductCodeSnapshot(supplierCandidate.supplierProduct().getSupplierProductCode());
        line.setQuantity(quantity);
        line.setBaseQuantity(quantity);
        line.setUnitPrice(BigDecimal.ZERO);
        line.setTaxRate(BigDecimal.ZERO);
        line.setTaxableAmount(BigDecimal.ZERO);
        line.setCgstRate(BigDecimal.ZERO);
        line.setCgstAmount(BigDecimal.ZERO);
        line.setSgstRate(BigDecimal.ZERO);
        line.setSgstAmount(BigDecimal.ZERO);
        line.setIgstRate(BigDecimal.ZERO);
        line.setIgstAmount(BigDecimal.ZERO);
        line.setCessRate(BigDecimal.ZERO);
        line.setCessAmount(BigDecimal.ZERO);
        line.setLineAmount(BigDecimal.ZERO);
        line.setReceivedBaseQuantity(BigDecimal.ZERO);
        purchaseOrderLineRepository.save(line);

        return new InventoryDtos.DraftPurchaseOrderSummaryResponse(
                order.getId(),
                order.getPoNumber(),
                order.getStatus(),
                supplierCandidate.supplier().getId(),
                supplierCandidate.supplier().getName(),
                quantity,
                order.getPoDate()
        );
    }

    public InventoryDtos.ReplenishmentTransferSummaryResponse createRecommendedTransfer(
            InventoryDtos.CreateReplenishmentTransferRequest request
    ) {
        accessGuard.assertOrganizationAccess(request.organizationId());
        subscriptionAccessService.assertFeature(request.organizationId(), "inventory");

        StoreProduct product = storeProductRepository.findById(request.storeProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Store product not found: " + request.storeProductId()));
        if (!request.organizationId().equals(product.getOrganizationId())) {
            throw new BusinessException("Store product does not belong to organization " + request.organizationId());
        }

        Warehouse sourceWarehouse = warehouseRepository.findByIdAndOrganizationId(request.sourceWarehouseId(), request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Source warehouse not found: " + request.sourceWarehouseId()));
        Warehouse targetWarehouse = warehouseRepository.findByIdAndOrganizationId(request.targetWarehouseId(), request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Target warehouse not found: " + request.targetWarehouseId()));
        if (sourceWarehouse.getId().equals(targetWarehouse.getId())) {
            throw new BusinessException("Source and target warehouse must be different");
        }

        accessGuard.assertBranchAccess(request.organizationId(), sourceWarehouse.getBranchId());
        accessGuard.assertBranchAccess(request.organizationId(), targetWarehouse.getBranchId());

        BigDecimal recommended = resolveRecommendedTransferQuantity(
                request.organizationId(),
                product,
                sourceWarehouse,
                targetWarehouse
        );
        BigDecimal quantity = safe(request.quantity());
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            quantity = recommended;
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("No transfer quantity available for product " + product.getSku());
        }

        var transfer = inventoryOperationsService.createTransfer(
                request.organizationId(),
                request.branchId() != null ? request.branchId() : targetWarehouse.getBranchId(),
                sourceWarehouse.getId(),
                targetWarehouse.getId(),
                List.of(new InventoryOperationsService.TransferLineCommand(
                        product.getId(),
                        product.getBaseUomId(),
                        null,
                        null,
                        quantity,
                        quantity
                ))
        );

        return new InventoryDtos.ReplenishmentTransferSummaryResponse(
                transfer.getId(),
                transfer.getTransferNumber(),
                transfer.getStatus(),
                transfer.getFromWarehouseId(),
                transfer.getToWarehouseId(),
                quantity,
                transfer.getTransferDate()
        );
    }

    private List<Warehouse> warehousesForScope(Long organizationId, Long branchId, Long warehouseId) {
        if (warehouseId != null) {
            return warehouseRepository.findByIdAndOrganizationId(warehouseId, organizationId).stream().toList();
        }
        if (branchId != null) {
            return warehouseRepository.findByOrganizationIdAndBranchIdOrderByIdAsc(organizationId, branchId);
        }
        return warehouseRepository.findByOrganizationIdOrderByBranchIdAscIdAsc(organizationId);
    }

    private Map<Long, Map<Long, BigDecimal>> aggregateAvailability(List<InventoryBalance> balances) {
        Map<Long, Map<Long, BigDecimal>> results = new HashMap<>();
        for (InventoryBalance balance : balances) {
            results.computeIfAbsent(balance.getWarehouseId(), ignored -> new HashMap<>())
                    .merge(balance.getProductId(), safe(balance.getAvailableBaseQuantity()), BigDecimal::add);
        }
        return results;
    }

    private Optional<TransferCandidate> resolveTransferCandidate(
            List<Warehouse> candidateWarehouses,
            Map<Long, Map<Long, BigDecimal>> availableByWarehouseAndProduct,
            Long targetWarehouseId,
            StoreProduct product,
            BigDecimal requestedQty
    ) {
        BigDecimal threshold = safe(product.getReorderLevelBaseQty()).max(safe(product.getMinStockBaseQty()));
        return candidateWarehouses.stream()
                .filter(candidate -> !candidate.getId().equals(targetWarehouseId))
                .map(candidate -> {
                    BigDecimal sourceAvailable = availableByWarehouseAndProduct
                            .getOrDefault(candidate.getId(), Map.of())
                            .getOrDefault(product.getId(), BigDecimal.ZERO);
                    BigDecimal excess = sourceAvailable.subtract(threshold);
                    if (excess.compareTo(BigDecimal.ZERO) <= 0) {
                        return null;
                    }
                    return new TransferCandidate(
                            candidate,
                            sourceAvailable,
                            excess,
                            requestedQty.min(excess)
                    );
                })
                .filter(candidate -> candidate != null && candidate.recommendedTransferQuantity().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(TransferCandidate::sourceExcess));
    }

    private Optional<SupplierCandidate> resolveSupplierCandidate(Long organizationId, StoreProduct storeProduct, LocalDate asOfDate) {
        StoreProductSupplierPreference preference = storeProductSupplierPreferenceRepository
                .findByOrganizationIdAndStoreProductIdAndIsActiveTrue(organizationId, storeProduct.getId())
                .orElse(null);
        if (preference != null) {
            Supplier supplier = supplierRepository.findById(preference.getSupplierId()).orElse(null);
            SupplierProduct supplierProduct = supplier == null ? null
                    : supplierProductRepository.findByIdAndOrganizationId(preference.getSupplierProductId(), organizationId).orElse(null);
            StoreSupplierTerms terms = supplier == null ? null
                    : storeSupplierTermsRepository.findByOrganizationIdAndSupplierId(organizationId, supplier.getId()).orElse(null);
            if (isEligibleMapping(storeProduct, asOfDate, supplier, supplierProduct, terms)) {
                return Optional.of(new SupplierCandidate(supplier, supplierProduct, terms));
            }
        }

        List<SupplierCandidate> eligible = new ArrayList<>();
        for (SupplierProduct supplierProduct : supplierProductRepository
                .findByOrganizationIdAndProductIdAndIsActiveTrueOrderByIsPreferredDescPriorityAscIdAsc(organizationId, storeProduct.getProductId())) {
            Supplier supplier = supplierRepository.findById(supplierProduct.getSupplierId()).orElse(null);
            StoreSupplierTerms terms = supplier == null ? null
                    : storeSupplierTermsRepository.findByOrganizationIdAndSupplierId(organizationId, supplier.getId()).orElse(null);
            if (isEligibleMapping(storeProduct, asOfDate, supplier, supplierProduct, terms)) {
                eligible.add(new SupplierCandidate(supplier, supplierProduct, terms));
            }
        }
        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        List<SupplierCandidate> preferredTerms = eligible.stream()
                .filter(candidate -> candidate.terms() != null && Boolean.TRUE.equals(candidate.terms().getIsPreferred()))
                .toList();
        if (preferredTerms.size() == 1) {
            return Optional.of(preferredTerms.get(0));
        }

        List<SupplierCandidate> preferredProducts = eligible.stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.supplierProduct().getIsPreferred()))
                .toList();
        if (preferredProducts.size() == 1) {
            return Optional.of(preferredProducts.get(0));
        }

        List<SupplierCandidate> priorityOne = eligible.stream()
                .filter(candidate -> candidate.supplierProduct().getPriority() != null && candidate.supplierProduct().getPriority() == 1)
                .toList();
        if (priorityOne.size() == 1) {
            return Optional.of(priorityOne.get(0));
        }

        return eligible.size() == 1 ? Optional.of(eligible.get(0)) : Optional.empty();
    }

    private SupplierCandidate resolveExplicitSupplier(Long organizationId, StoreProduct product, Long supplierId, LocalDate asOfDate) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
        if (!organizationId.equals(supplier.getOrganizationId())) {
            throw new BusinessException("Supplier does not belong to organization " + organizationId);
        }
        List<SupplierProduct> options = supplierProductRepository
                .findByOrganizationIdAndSupplierIdAndProductIdAndIsActiveTrueOrderByIsPreferredDescPriorityAscIdAsc(
                        organizationId,
                        supplierId,
                        product.getProductId()
                );
        if (options.isEmpty()) {
            throw new BusinessException("Supplier is not mapped to product " + product.getSku());
        }
        StoreSupplierTerms terms = storeSupplierTermsRepository.findByOrganizationIdAndSupplierId(organizationId, supplierId).orElse(null);
        SupplierProduct supplierProduct = options.get(0);
        if (!isEligibleMapping(product, asOfDate, supplier, supplierProduct, terms)) {
            throw new BusinessException("Supplier mapping is not active for product " + product.getSku());
        }
        return new SupplierCandidate(supplier, supplierProduct, terms);
    }

    private boolean isEligibleMapping(
            StoreProduct storeProduct,
            LocalDate asOfDate,
            Supplier supplier,
            SupplierProduct supplierProduct,
            StoreSupplierTerms terms
    ) {
        if (supplier == null || supplierProduct == null) {
            return false;
        }
        if (!supplier.getOrganizationId().equals(storeProduct.getOrganizationId())) {
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(supplier.getStatus()) || !Boolean.TRUE.equals(supplierProduct.getIsActive())) {
            return false;
        }
        if (!storeProduct.getProductId().equals(supplierProduct.getProductId())) {
            return false;
        }
        return isActiveTerms(terms, asOfDate);
    }

    private boolean isActiveTerms(StoreSupplierTerms terms, LocalDate asOfDate) {
        if (terms == null || !Boolean.TRUE.equals(terms.getIsActive())) {
            return false;
        }
        if (terms.getContractStart() != null && asOfDate.isBefore(terms.getContractStart())) {
            return false;
        }
        return terms.getContractEnd() == null || !asOfDate.isAfter(terms.getContractEnd());
    }

    private boolean hasOpenPurchaseOrderForProduct(Long organizationId, Long supplierId, Long storeProductId) {
        List<PurchaseOrder> openOrders = purchaseOrderRepository.findByOrganizationIdAndSupplierIdAndStatusInOrderByPoDateDescIdDesc(
                organizationId,
                supplierId,
                List.of(
                        ErpDocumentStatuses.DRAFT,
                        ErpDocumentStatuses.SUBMITTED,
                        ErpDocumentStatuses.PENDING_APPROVAL,
                        ErpDocumentStatuses.APPROVED,
                        ErpDocumentStatuses.PARTIALLY_RECEIVED
                )
        );
        if (openOrders.isEmpty()) {
            return false;
        }
        List<Long> orderIds = openOrders.stream().map(PurchaseOrder::getId).toList();
        return !purchaseOrderLineRepository.findByPurchaseOrderIdInAndProductId(orderIds, storeProductId).isEmpty();
    }

    private BigDecimal resolveRecommendedTransferQuantity(
            Long organizationId,
            StoreProduct product,
            Warehouse sourceWarehouse,
            Warehouse targetWarehouse
    ) {
        BigDecimal targetAvailable = availableForWarehouse(organizationId, targetWarehouse.getId(), product.getId());
        BigDecimal required = recommendedReorderQuantity(
                targetAvailable,
                safe(product.getReorderLevelBaseQty()),
                safe(product.getMinStockBaseQty())
        );
        BigDecimal sourceAvailable = availableForWarehouse(organizationId, sourceWarehouse.getId(), product.getId());
        BigDecimal sourceThreshold = safe(product.getReorderLevelBaseQty()).max(safe(product.getMinStockBaseQty()));
        BigDecimal sourceExcess = sourceAvailable.subtract(sourceThreshold);
        if (sourceExcess.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return required.min(sourceExcess);
    }

    private boolean isWarehouseTransferEligible(StoreProduct product) {
        return !Boolean.TRUE.equals(product.getSerialTrackingEnabled())
                && !Boolean.TRUE.equals(product.getBatchTrackingEnabled());
    }

    private BigDecimal availableForWarehouse(Long organizationId, Long warehouseId, Long storeProductId) {
        return inventoryBalanceRepository.findByOrganizationIdAndProductIdAndWarehouseId(organizationId, storeProductId, warehouseId).stream()
                .map(InventoryBalance::getAvailableBaseQuantity)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Warehouse resolveWarehouseForPurchase(Long organizationId, Long branchId, Long warehouseId) {
        if (warehouseId != null) {
            Warehouse warehouse = warehouseRepository.findByIdAndOrganizationId(warehouseId, organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
            accessGuard.assertBranchAccess(organizationId, warehouse.getBranchId());
            return warehouse;
        }
        if (branchId != null) {
            accessGuard.assertBranchAccess(organizationId, branchId);
            return warehouseRepository.findByOrganizationIdAndBranchIdOrderByIdAsc(organizationId, branchId).stream()
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("No warehouse found for branch " + branchId));
        }
        Long defaultBranchId = ErpSecurityUtils.currentBranchId().orElse(null);
        if (defaultBranchId != null) {
            return warehouseRepository.findByOrganizationIdAndBranchIdOrderByIdAsc(organizationId, defaultBranchId).stream()
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("No warehouse found for branch " + defaultBranchId));
        }
        Branch branch = branchRepository.findByOrganizationIdAndIsActiveTrueOrderByIdAsc(organizationId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("No active branch found for organization " + organizationId));
        return warehouseRepository.findByOrganizationIdAndBranchIdOrderByIdAsc(organizationId, branch.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("No warehouse found for branch " + branch.getId()));
    }

    private BigDecimal recommendedReorderQuantity(BigDecimal available, BigDecimal reorderLevel, BigDecimal minStock) {
        BigDecimal targetLevel = reorderLevel.max(minStock);
        if (targetLevel.compareTo(BigDecimal.ZERO) <= 0) {
            targetLevel = BigDecimal.ONE;
        }
        BigDecimal buffer = reorderLevel.compareTo(BigDecimal.ZERO) > 0 ? reorderLevel : BigDecimal.ONE;
        BigDecimal targetWithBuffer = targetLevel.add(buffer);
        BigDecimal reorderQuantity = targetWithBuffer.subtract(available);
        return reorderQuantity.compareTo(BigDecimal.ONE) < 0 ? BigDecimal.ONE : reorderQuantity;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record TransferCandidate(
            Warehouse sourceWarehouse,
            BigDecimal sourceAvailable,
            BigDecimal sourceExcess,
            BigDecimal recommendedTransferQuantity
    ) {}

    private record SupplierCandidate(
            Supplier supplier,
            SupplierProduct supplierProduct,
            StoreSupplierTerms terms
    ) {}
}
