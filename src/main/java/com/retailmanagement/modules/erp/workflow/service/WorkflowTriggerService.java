package com.retailmanagement.modules.erp.workflow.service;

import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.entity.StoreSupplierTerms;
import com.retailmanagement.modules.erp.party.entity.Supplier;
import com.retailmanagement.modules.erp.party.entity.SupplierProduct;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.party.repository.StoreSupplierTermsRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierProductRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrder;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrderLine;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseOrderLineRepository;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseOrderRepository;
import com.retailmanagement.modules.erp.sales.entity.ProductOwnership;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptAllocationRepository;
import com.retailmanagement.modules.erp.sales.repository.ProductOwnershipRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.tax.dto.TaxDtos;
import com.retailmanagement.modules.erp.tax.service.TaxRegistrationService;
import com.retailmanagement.modules.erp.workflow.dto.WorkflowTriggerDtos;
import com.retailmanagement.modules.notification.dto.request.NotificationRequest;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import com.retailmanagement.modules.notification.enums.NotificationPriority;
import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.model.Notification;
import com.retailmanagement.modules.notification.repository.NotificationRepository;
import com.retailmanagement.modules.notification.service.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTriggerService {

    private static final String TRIGGER_LOW_STOCK = "LOW_STOCK_REORDER";
    private static final String TRIGGER_OVERDUE_DUE = "CUSTOMER_DUE_OVERDUE";
    private static final String TRIGGER_WARRANTY_EXPIRY = "WARRANTY_EXPIRY_SOON";
    private static final String TRIGGER_GST_THRESHOLD = "GST_THRESHOLD_ALERT";

    private final ErpAccessGuard accessGuard;
    private final StoreProductRepository storeProductRepository;
    private final BranchRepository branchRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final CustomerReceiptAllocationRepository customerReceiptAllocationRepository;
    private final ProductOwnershipRepository productOwnershipRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final StoreSupplierTermsRepository storeSupplierTermsRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final TaxRegistrationService taxRegistrationService;
    private final OrganizationRepository organizationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public WorkflowTriggerDtos.WorkflowTriggerReviewResponse review(Long organizationId, LocalDate asOfDate) {
        accessGuard.assertOrganizationAccess(organizationId);
        LocalDate effectiveDate = asOfDate == null ? LocalDate.now() : asOfDate;
        List<TriggerCandidate> triggers = findTriggers(organizationId, effectiveDate);
        return toReviewResponse(organizationId, effectiveDate, triggers);
    }

    @Transactional
    public WorkflowTriggerDtos.WorkflowTriggerDispatchResponse dispatch(Long organizationId, LocalDate asOfDate) {
        accessGuard.assertOrganizationAccess(organizationId);
        LocalDate effectiveDate = asOfDate == null ? LocalDate.now() : asOfDate;
        List<TriggerCandidate> triggers = findTriggers(organizationId, effectiveDate);
        List<TriggerCandidate> dispatched = new ArrayList<>();
        for (TriggerCandidate trigger : triggers) {
            TriggerCandidate enrichedTrigger = maybeCreateDraftPurchaseOrder(organizationId, effectiveDate, trigger);
            if (alreadyDispatchedToday(enrichedTrigger.referenceType(), enrichedTrigger.referenceId(), enrichedTrigger.type(), effectiveDate)) {
                continue;
            }
            dispatchTriggerNotification(organizationId, enrichedTrigger);
            dispatched.add(enrichedTrigger);
        }
        return new WorkflowTriggerDtos.WorkflowTriggerDispatchResponse(
                organizationId,
                effectiveDate,
                triggers.size(),
                dispatched.size(),
                dispatched.stream().map(this::toItemResponse).toList()
        );
    }

    @Scheduled(fixedDelayString = "${erp.workflow.scan-ms:3600000}")
    public void dispatchScheduledTriggers() {
        LocalDate today = LocalDate.now();
        for (Organization organization : organizationRepository.findAll()) {
            if (!Boolean.TRUE.equals(organization.getIsActive())) {
                continue;
            }
            try {
                dispatchInternally(organization.getId(), today);
            } catch (Exception ex) {
                log.warn("Workflow trigger scan failed for organization {}: {}", organization.getId(), ex.getMessage());
            }
        }
    }

    @Transactional
    private void dispatchInternally(Long organizationId, LocalDate asOfDate) {
        List<TriggerCandidate> triggers = findTriggers(organizationId, asOfDate);
        for (TriggerCandidate trigger : triggers) {
            TriggerCandidate enrichedTrigger = maybeCreateDraftPurchaseOrder(organizationId, asOfDate, trigger);
            if (alreadyDispatchedToday(enrichedTrigger.referenceType(), enrichedTrigger.referenceId(), enrichedTrigger.type(), asOfDate)) {
                continue;
            }
            dispatchTriggerNotification(organizationId, enrichedTrigger);
        }
    }

    private List<TriggerCandidate> findTriggers(Long organizationId, LocalDate asOfDate) {
        List<TriggerCandidate> triggers = new ArrayList<>();
        triggers.addAll(lowStockTriggers(organizationId));
        triggers.addAll(overdueDueTriggers(organizationId, asOfDate));
        triggers.addAll(warrantyExpiryTriggers(organizationId, asOfDate));
        gstThresholdTrigger(organizationId, asOfDate).ifPresent(triggers::add);
        triggers.sort(Comparator.comparing(TriggerCandidate::severityRank).thenComparing(TriggerCandidate::title));
        return triggers;
    }

    private List<TriggerCandidate> lowStockTriggers(Long organizationId) {
        Map<Long, BigDecimal> availableByProduct = new HashMap<>();
        for (InventoryBalance balance : inventoryBalanceRepository.findByOrganizationId(organizationId)) {
            availableByProduct.merge(balance.getProductId(), safe(balance.getAvailableBaseQuantity()), BigDecimal::add);
        }

        List<TriggerCandidate> triggers = new ArrayList<>();
        for (StoreProduct product : storeProductRepository.findByOrganizationId(organizationId)) {
            if (!Boolean.TRUE.equals(product.getIsActive())) {
                continue;
            }
            BigDecimal reorderLevel = safe(product.getReorderLevelBaseQty());
            if (reorderLevel.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal available = availableByProduct.getOrDefault(product.getId(), BigDecimal.ZERO);
            if (available.compareTo(reorderLevel) > 0) {
                continue;
            }
            String severity = available.compareTo(BigDecimal.ZERO) <= 0 ? "CRITICAL" : "WARNING";
            Map<String, Object> data = Map.of(
                    "sku", product.getSku(),
                    "productName", product.getName(),
                    "availableBaseQty", available,
                    "reorderLevelBaseQty", reorderLevel,
                    "minStockBaseQty", safe(product.getMinStockBaseQty())
            );
            triggers.add(new TriggerCandidate(
                    TRIGGER_LOW_STOCK,
                    severity,
                    "Reorder needed for " + product.getSku(),
                    product.getName() + " is at " + available + " base qty against reorder level " + reorderLevel + ".",
                    "WORKFLOW_REORDER",
                    product.getId(),
                    null,
                    null,
                    data,
                    NotificationType.LOW_STOCK_ALERT,
                    "CRITICAL".equals(severity) ? NotificationPriority.URGENT : NotificationPriority.HIGH
            ));
        }
        return triggers;
    }

    private List<TriggerCandidate> overdueDueTriggers(Long organizationId, LocalDate asOfDate) {
        Map<Long, Customer> customers = customerRepository.findByOrganizationId(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(Customer::getId, c -> c));
        List<TriggerCandidate> triggers = new ArrayList<>();
        for (SalesInvoice invoice : salesInvoiceRepository.findByOrganizationIdOrderByDueDateAscIdAsc(organizationId)) {
            if (invoice.getDueDate() == null || !invoice.getDueDate().isBefore(asOfDate)) {
                continue;
            }
            if ("CANCELLED".equals(invoice.getStatus()) || "PAID".equals(invoice.getStatus())) {
                continue;
            }
            BigDecimal allocated = customerReceiptAllocationRepository.findBySalesInvoiceIdOrderByIdAsc(invoice.getId()).stream()
                    .map(a -> safe(a.getAllocatedAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = safe(invoice.getTotalAmount()).subtract(allocated).max(BigDecimal.ZERO);
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            long overdueDays = ChronoUnit.DAYS.between(invoice.getDueDate(), asOfDate);
            Customer customer = customers.get(invoice.getCustomerId());
            String customerName = customer == null ? "Customer " + invoice.getCustomerId() : customer.getTradeName() != null ? customer.getTradeName() : customer.getLegalName();
            String severity = overdueDays >= 30 ? "CRITICAL" : "WARNING";
            Map<String, Object> data = Map.of(
                    "invoiceNumber", invoice.getInvoiceNumber(),
                    "customerName", customerName,
                    "outstandingAmount", outstanding,
                    "overdueDays", overdueDays
            );
            triggers.add(new TriggerCandidate(
                    TRIGGER_OVERDUE_DUE,
                    severity,
                    "Overdue due for " + customerName,
                    "Invoice " + invoice.getInvoiceNumber() + " is overdue by " + overdueDays + " days with outstanding " + outstanding + ".",
                    "WORKFLOW_OVERDUE_DUE",
                    invoice.getId(),
                    outstanding,
                    invoice.getDueDate(),
                    data,
                    NotificationType.DUE_OVERDUE,
                    "CRITICAL".equals(severity) ? NotificationPriority.URGENT : NotificationPriority.HIGH
            ));
        }
        return triggers;
    }

    private List<TriggerCandidate> warrantyExpiryTriggers(Long organizationId, LocalDate asOfDate) {
        List<TriggerCandidate> triggers = new ArrayList<>();
        for (ProductOwnership ownership : productOwnershipRepository.findByOrganizationId(organizationId)) {
            if (!"ACTIVE".equals(ownership.getStatus()) || ownership.getWarrantyEndDate() == null) {
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(asOfDate, ownership.getWarrantyEndDate());
            if (daysLeft < 0 || daysLeft > 30) {
                continue;
            }
            String severity = daysLeft <= 7 ? "CRITICAL" : "WARNING";
            Map<String, Object> data = Map.of(
                    "productId", ownership.getProductId(),
                    "customerId", ownership.getCustomerId(),
                    "serialNumberId", ownership.getSerialNumberId(),
                    "warrantyEndDate", ownership.getWarrantyEndDate(),
                    "daysLeft", daysLeft
            );
            triggers.add(new TriggerCandidate(
                    TRIGGER_WARRANTY_EXPIRY,
                    severity,
                    "Warranty nearing expiry",
                    "Product ownership " + ownership.getId() + " warranty ends on " + ownership.getWarrantyEndDate() + " (" + daysLeft + " days left).",
                    "WORKFLOW_WARRANTY_EXPIRY",
                    ownership.getId(),
                    null,
                    ownership.getWarrantyEndDate(),
                    data,
                    NotificationType.SYSTEM_ALERT,
                    "CRITICAL".equals(severity) ? NotificationPriority.HIGH : NotificationPriority.MEDIUM
            ));
        }
        return triggers;
    }

    private java.util.Optional<TriggerCandidate> gstThresholdTrigger(Long organizationId, LocalDate asOfDate) {
        TaxDtos.GstThresholdStatusResponse status = taxRegistrationService.thresholdStatus(organizationId, asOfDate);
        if (!Boolean.TRUE.equals(status.alertEnabled())) {
            return java.util.Optional.empty();
        }
        if (!List.of("MEDIUM", "HIGH", "CRITICAL").contains(status.alertLevel())) {
            return java.util.Optional.empty();
        }
        String severity = "CRITICAL".equals(status.alertLevel()) ? "CRITICAL" : "WARNING";
        Map<String, Object> data = Map.of(
                "financialYearTurnover", safe(status.financialYearTurnover()),
                "gstThresholdAmount", safe(status.gstThresholdAmount()),
                "utilizationRatio", safe(status.utilizationRatio()),
                "alertLevel", status.alertLevel()
        );
        return java.util.Optional.of(new TriggerCandidate(
                TRIGGER_GST_THRESHOLD,
                severity,
                "GST threshold alert",
                status.message(),
                "WORKFLOW_GST_THRESHOLD",
                organizationId,
                safe(status.financialYearTurnover()),
                asOfDate,
                data,
                NotificationType.SYSTEM_ALERT,
                "CRITICAL".equals(severity) ? NotificationPriority.URGENT : NotificationPriority.HIGH
        ));
    }

    private boolean alreadyDispatchedToday(String referenceType, Long referenceId, String triggerType, LocalDate asOfDate) {
        return notificationRepository.findByReference(referenceType, referenceId).stream()
                .anyMatch(notification -> notification.getType() != null
                        && notification.getCreatedAt() != null
                        && notification.getCreatedAt().toLocalDate().isEqual(asOfDate)
                        && triggerType.equals(triggerType(notification)));
    }

    private void dispatchTriggerNotification(Long organizationId, TriggerCandidate trigger) {
        List<User> recipients = userRepository.findActiveByOrganizationIdAndRoleCodeIn(
                organizationId,
                recipientRolesFor(trigger.type())
        );
        if (recipients.isEmpty()) {
            NotificationRequest request = buildNotificationRequest(null, trigger);
            notificationService.sendNotification(request);
            return;
        }
        for (User recipient : recipients) {
            NotificationRequest request = buildNotificationRequest(recipient.getId(), trigger);
            notificationService.sendNotification(request);
        }
    }

    private NotificationRequest buildNotificationRequest(Long userId, TriggerCandidate trigger) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setType(trigger.notificationType());
        request.setChannel(NotificationChannel.IN_APP);
        request.setPriority(trigger.priority());
        request.setTitle(trigger.title());
        request.setContent(trigger.message());
        request.setReferenceType(trigger.referenceType());
        request.setReferenceId(trigger.referenceId());
        request.setScheduledFor(LocalDateTime.now());
        request.setData(trigger.data());
        return request;
    }

    private Collection<String> recipientRolesFor(String triggerType) {
        return switch (triggerType) {
            case TRIGGER_LOW_STOCK, TRIGGER_WARRANTY_EXPIRY -> List.of("OWNER", "ADMIN", "STORE_MANAGER");
            case TRIGGER_OVERDUE_DUE, TRIGGER_GST_THRESHOLD -> List.of("OWNER", "ADMIN", "ACCOUNTANT");
            default -> List.of("OWNER", "ADMIN");
        };
    }

    private TriggerCandidate maybeCreateDraftPurchaseOrder(Long organizationId, LocalDate asOfDate, TriggerCandidate trigger) {
        if (!TRIGGER_LOW_STOCK.equals(trigger.type())) {
            return trigger;
        }
        Long storeProductId = trigger.referenceId();
        if (storeProductId == null) {
            return trigger;
        }
        return createDraftPurchaseOrderForLowStock(organizationId, asOfDate, storeProductId, trigger)
                .map(result -> withDraftPurchaseOrder(trigger, result))
                .orElse(trigger);
    }

    private java.util.Optional<DraftPurchaseOrderResult> createDraftPurchaseOrderForLowStock(Long organizationId,
                                                                                              LocalDate asOfDate,
                                                                                              Long storeProductId,
                                                                                              TriggerCandidate trigger) {
        StoreProduct storeProduct = storeProductRepository.findById(storeProductId)
                .orElse(null);
        if (storeProduct == null || !organizationId.equals(storeProduct.getOrganizationId()) || !Boolean.TRUE.equals(storeProduct.getIsActive())) {
            return java.util.Optional.empty();
        }
        Branch branch = branchRepository.findByOrganizationIdAndIsActiveTrueOrderByIdAsc(organizationId).stream()
                .max(Comparator.comparing(branchCandidate -> shortageForBranch(organizationId, branchCandidate.getId(), storeProduct)))
                .orElse(null);
        if (branch == null) {
            return java.util.Optional.empty();
        }

        CandidateSupplierMapping mapping = resolveSupplierMapping(organizationId, storeProduct, asOfDate).orElse(null);
        if (mapping == null) {
            return java.util.Optional.empty();
        }
        if (hasOpenPurchaseOrderForProduct(organizationId, mapping.supplier().getId(), storeProduct.getId())) {
            return java.util.Optional.empty();
        }

        BigDecimal available = safeBigDecimal(trigger.data().get("availableBaseQty"));
        BigDecimal reorderLevel = safeBigDecimal(trigger.data().get("reorderLevelBaseQty"));
        BigDecimal minStock = safeBigDecimal(trigger.data().get("minStockBaseQty"));
        BigDecimal reorderQuantity = recommendedReorderQuantity(available, reorderLevel, minStock);

        String poNumber = "AUTO-PO-" + asOfDate.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
        PurchaseOrder order = new PurchaseOrder();
        order.setOrganizationId(organizationId);
        order.setBranchId(branch.getId());
        order.setSupplierId(mapping.supplier().getId());
        order.setPoNumber(poNumber);
        order.setPoDate(asOfDate);
        order.setSupplierGstin(mapping.supplier().getGstin());
        order.setStatus(ErpDocumentStatuses.DRAFT);
        order.setRemarks("Auto-generated from low stock workflow trigger for " + storeProduct.getSku());
        order.setSubtotal(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order = purchaseOrderRepository.save(order);

        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setPurchaseOrderId(order.getId());
        line.setProductId(storeProduct.getId());
        line.setSupplierProductId(mapping.supplierProduct().getId());
        line.setProductMasterId(storeProduct.getProductId());
        line.setUomId(storeProduct.getBaseUomId());
        line.setSkuSnapshot(storeProduct.getSku());
        line.setProductNameSnapshot(storeProduct.getName());
        line.setSupplierProductCodeSnapshot(mapping.supplierProduct().getSupplierProductCode());
        line.setQuantity(reorderQuantity);
        line.setBaseQuantity(reorderQuantity);
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

        return java.util.Optional.of(new DraftPurchaseOrderResult(order.getId(), order.getPoNumber(), mapping.supplier().getId(), reorderQuantity));
    }

    private java.util.Optional<CandidateSupplierMapping> resolveSupplierMapping(Long organizationId, StoreProduct storeProduct, LocalDate asOfDate) {
        List<CandidateSupplierMapping> eligibleMappings = new ArrayList<>();
        for (SupplierProduct supplierProduct : supplierProductRepository
                .findByOrganizationIdAndProductIdAndIsActiveTrueOrderByIsPreferredDescPriorityAscIdAsc(organizationId, storeProduct.getProductId())) {
            Supplier supplier = supplierRepository.findByOrganizationIdAndId(organizationId, supplierProduct.getSupplierId())
                    .orElse(null);
            if (supplier == null || !"ACTIVE".equalsIgnoreCase(supplier.getStatus())) {
                continue;
            }
            StoreSupplierTerms terms = storeSupplierTermsRepository.findByOrganizationIdAndSupplierId(organizationId, supplierProduct.getSupplierId())
                    .orElse(null);
            if (!isActiveTerms(terms, asOfDate)) {
                continue;
            }
            eligibleMappings.add(new CandidateSupplierMapping(supplier, supplierProduct, terms));
        }
        if (eligibleMappings.isEmpty()) {
            return java.util.Optional.empty();
        }
        List<CandidateSupplierMapping> preferredTerms = eligibleMappings.stream()
                .filter(mapping -> Boolean.TRUE.equals(mapping.terms().getIsPreferred()))
                .toList();
        if (preferredTerms.size() == 1) {
            return java.util.Optional.of(preferredTerms.get(0));
        }
        List<CandidateSupplierMapping> preferredProducts = eligibleMappings.stream()
                .filter(mapping -> Boolean.TRUE.equals(mapping.supplierProduct().getIsPreferred()))
                .toList();
        if (preferredProducts.size() == 1) {
            return java.util.Optional.of(preferredProducts.get(0));
        }
        List<CandidateSupplierMapping> priorityOne = eligibleMappings.stream()
                .filter(mapping -> mapping.supplierProduct().getPriority() != null && mapping.supplierProduct().getPriority() == 1)
                .toList();
        if (priorityOne.size() == 1) {
            return java.util.Optional.of(priorityOne.get(0));
        }
        return eligibleMappings.size() == 1 ? java.util.Optional.of(eligibleMappings.get(0)) : java.util.Optional.empty();
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

    private BigDecimal shortageForBranch(Long organizationId, Long branchId, StoreProduct storeProduct) {
        BigDecimal branchAvailable = inventoryBalanceRepository.findByOrganizationId(organizationId).stream()
                .filter(balance -> branchId.equals(balance.getBranchId()))
                .filter(balance -> storeProduct.getId().equals(balance.getProductId()))
                .map(balance -> safe(balance.getAvailableBaseQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return recommendedReorderQuantity(
                branchAvailable,
                safe(storeProduct.getReorderLevelBaseQty()),
                safe(storeProduct.getMinStockBaseQty())
        );
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

    private boolean isActiveTerms(StoreSupplierTerms terms, LocalDate asOfDate) {
        if (terms == null || !Boolean.TRUE.equals(terms.getIsActive())) {
            return false;
        }
        if (terms.getContractStart() != null && asOfDate.isBefore(terms.getContractStart())) {
            return false;
        }
        return terms.getContractEnd() == null || !asOfDate.isAfter(terms.getContractEnd());
    }

    private TriggerCandidate withDraftPurchaseOrder(TriggerCandidate trigger, DraftPurchaseOrderResult result) {
        Map<String, Object> data = new HashMap<>(trigger.data());
        data.put("draftPurchaseOrderId", result.purchaseOrderId());
        data.put("draftPurchaseOrderNumber", result.purchaseOrderNumber());
        data.put("draftPurchaseSupplierId", result.supplierId());
        data.put("draftPurchaseOrderQuantity", result.quantity());
        return new TriggerCandidate(
                trigger.type(),
                trigger.severity(),
                trigger.title(),
                trigger.message() + " Draft PO " + result.purchaseOrderNumber() + " was created.",
                trigger.referenceType(),
                trigger.referenceId(),
                trigger.amount(),
                trigger.dueDate(),
                data,
                trigger.notificationType(),
                trigger.priority()
        );
    }

    private BigDecimal safeBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private String triggerType(Notification notification) {
        return switch (notification.getReferenceType()) {
            case "WORKFLOW_REORDER" -> TRIGGER_LOW_STOCK;
            case "WORKFLOW_OVERDUE_DUE" -> TRIGGER_OVERDUE_DUE;
            case "WORKFLOW_WARRANTY_EXPIRY" -> TRIGGER_WARRANTY_EXPIRY;
            case "WORKFLOW_GST_THRESHOLD" -> TRIGGER_GST_THRESHOLD;
            default -> "";
        };
    }

    private WorkflowTriggerDtos.WorkflowTriggerReviewResponse toReviewResponse(Long organizationId, LocalDate asOfDate, List<TriggerCandidate> triggers) {
        int criticalCount = (int) triggers.stream().filter(t -> "CRITICAL".equals(t.severity())).count();
        int warningCount = (int) triggers.stream().filter(t -> "WARNING".equals(t.severity())).count();
        return new WorkflowTriggerDtos.WorkflowTriggerReviewResponse(
                organizationId,
                asOfDate,
                triggers.size(),
                criticalCount,
                warningCount,
                triggers.stream().map(this::toItemResponse).toList()
        );
    }

    private WorkflowTriggerDtos.WorkflowTriggerItemResponse toItemResponse(TriggerCandidate trigger) {
        return new WorkflowTriggerDtos.WorkflowTriggerItemResponse(
                trigger.type(),
                trigger.severity(),
                trigger.title(),
                trigger.message(),
                trigger.referenceType(),
                trigger.referenceId(),
                trigger.amount(),
                trigger.dueDate(),
                trigger.data()
        );
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record CandidateSupplierMapping(
            Supplier supplier,
            SupplierProduct supplierProduct,
            StoreSupplierTerms terms
    ) {}

    private record DraftPurchaseOrderResult(
            Long purchaseOrderId,
            String purchaseOrderNumber,
            Long supplierId,
            BigDecimal quantity
    ) {}

    private record TriggerCandidate(
            String type,
            String severity,
            String title,
            String message,
            String referenceType,
            Long referenceId,
            BigDecimal amount,
            LocalDate dueDate,
            Map<String, Object> data,
            NotificationType notificationType,
            NotificationPriority priority
    ) {
        int severityRank() {
            return "CRITICAL".equals(severity) ? 0 : 1;
        }
    }
}
