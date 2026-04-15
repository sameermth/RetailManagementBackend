package com.retailmanagement.modules.erp.pos.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.dto.ProductScanResponse;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.service.ProductService;
import com.retailmanagement.modules.erp.catalog.service.StoreProductPricingService;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.entity.Warehouse;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.foundation.repository.WarehouseRepository;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBatchRepository;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.pos.dto.PosDtos;
import com.retailmanagement.modules.erp.pos.entity.PosSession;
import com.retailmanagement.modules.erp.pos.repository.PosSessionRepository;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesDtos;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesResponses;
import com.retailmanagement.modules.erp.sales.entity.CustomerReceipt;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.sales.service.ErpSalesService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PosService {

    private static final String WALK_IN_CUSTOMER_PREFIX = "WALKIN-";

    private final PosSessionRepository posSessionRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreProductRepository storeProductRepository;
    private final ProductService productService;
    private final StoreProductPricingService storeProductPricingService;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final CustomerReceiptRepository customerReceiptRepository;
    private final CustomerRepository customerRepository;
    private final ErpSalesService erpSalesService;
    private final ErpAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public List<PosDtos.PosSessionSummaryResponse> listSessions(Long organizationId, Long branchId, Long warehouseId, String status) {
        accessGuard.assertOrganizationAccess(organizationId);
        List<PosSession> sessions = posSessionRepository.findByOrganizationIdOrderByOpenedAtDescIdDesc(organizationId).stream()
                .filter(session -> branchId == null || Objects.equals(branchId, session.getBranchId()))
                .filter(session -> warehouseId == null || Objects.equals(warehouseId, session.getWarehouseId()))
                .filter(session -> normalizeStatus(status) == null || normalizeStatus(status).equalsIgnoreCase(session.getStatus()))
                .toList();
        return sessions.stream()
                .map(this::toSessionSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PosDtos.PosSessionResponse getActiveSession(Long organizationId, Long branchId, Long warehouseId) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        Long userId = currentUserId();
        PosSession session = posSessionRepository
                .findFirstByOrganizationIdAndBranchIdAndWarehouseIdAndOpenedByUserIdAndStatusOrderByOpenedAtDescIdDesc(
                        organizationId,
                        branchId,
                        warehouseId,
                        userId,
                        ErpDocumentStatuses.OPEN
                )
                .orElseThrow(() -> new ResourceNotFoundException("No active POS session found"));
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public PosDtos.PosSessionResponse getSession(Long sessionId) {
        return toSessionResponse(requireSession(sessionId));
    }

    @Transactional(readOnly = true)
    public List<PosDtos.PosCatalogSearchItemResponse> searchCatalog(Long sessionId, String query, Long customerId, Integer limit) {
        PosSession session = requireSession(sessionId);
        String searchTerm = trimToNull(query);
        if (searchTerm == null) {
            throw new BusinessException("Search value is required");
        }

        int maxResults = limit == null ? 20 : Math.max(1, Math.min(limit, 50));
        String normalizedQuery = searchTerm.toLowerCase(Locale.ROOT);

        return storeProductRepository.searchActiveForPos(session.getOrganizationId(), searchTerm).stream()
                .filter(product -> isSellableInSession(session, product))
                .sorted(posSearchComparator(normalizedQuery))
                .limit(maxResults)
                .map(product -> toPosSearchItem(session, product, customerId, normalizedQuery))
                .toList();
    }

    public PosDtos.PosSessionResponse openSession(PosDtos.OpenPosSessionRequest request) {
        Long organizationId = requiredOrganizationId(request.organizationId());
        Warehouse warehouse = warehouseRepository.findByIdAndOrganizationId(request.warehouseId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.warehouseId()));
        Long branchId = request.branchId() == null ? warehouse.getBranchId() : request.branchId();
        accessGuard.assertBranchAccess(organizationId, branchId);
        if (!Objects.equals(branchId, warehouse.getBranchId())) {
            throw new BusinessException("Warehouse does not belong to branch " + branchId);
        }

        Long userId = currentUserId();
        if (posSessionRepository.existsByOrganizationIdAndBranchIdAndWarehouseIdAndOpenedByUserIdAndStatus(
                organizationId, branchId, warehouse.getId(), userId, ErpDocumentStatuses.OPEN)) {
            throw new BusinessException("An active POS session already exists for this cashier and warehouse");
        }
        String terminalName = trimToNull(request.terminalName());
        if (terminalName != null && posSessionRepository.existsByOrganizationIdAndBranchIdAndWarehouseIdAndTerminalNameIgnoreCaseAndStatus(
                organizationId, branchId, warehouse.getId(), terminalName, ErpDocumentStatuses.OPEN)) {
            throw new BusinessException("An active POS session already exists for terminal " + terminalName);
        }

        PosSession session = new PosSession();
        session.setOrganizationId(organizationId);
        session.setBranchId(branchId);
        session.setWarehouseId(warehouse.getId());
        session.setSessionNumber("POS-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis());
        session.setTerminalName(terminalName);
        session.setOpenedByUserId(userId);
        session.setOpenedByUsername(ErpSecurityUtils.currentUsername().orElse("unknown"));
        session.setOpenedAt(LocalDateTime.now());
        session.setOpeningCashAmount(zeroIfNull(request.openingCashAmount()).setScale(2, RoundingMode.HALF_UP));
        session.setOpeningNotes(trimToNull(request.openingNotes()));
        session.setStatus(ErpDocumentStatuses.OPEN);
        session = posSessionRepository.save(session);
        return toSessionResponse(session);
    }

    public PosDtos.PosSessionResponse closeSession(Long sessionId, PosDtos.ClosePosSessionRequest request) {
        PosSession session = requireOpenSession(sessionId);
        if (request != null) {
            if (request.organizationId() != null && !Objects.equals(request.organizationId(), session.getOrganizationId())) {
                throw new BusinessException("POS session does not belong to organization " + request.organizationId());
            }
            if (request.branchId() != null && !Objects.equals(request.branchId(), session.getBranchId())) {
                throw new BusinessException("POS session does not belong to branch " + request.branchId());
            }
        }

        PosFinancialSummary summary = calculateFinancialSummary(session);
        BigDecimal countedCash = request == null || request.countedClosingCashAmount() == null
                ? summary.expectedClosingCashAmount()
                : request.countedClosingCashAmount().setScale(2, RoundingMode.HALF_UP);
        session.setExpectedClosingCashAmount(summary.expectedClosingCashAmount());
        session.setCountedClosingCashAmount(countedCash);
        session.setCashVarianceAmount(countedCash.subtract(summary.expectedClosingCashAmount()).setScale(2, RoundingMode.HALF_UP));
        session.setStatus(ErpDocumentStatuses.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        session.setClosedByUserId(currentUserId());
        session.setClosedByUsername(ErpSecurityUtils.currentUsername().orElse("unknown"));
        session.setClosingNotes(request == null ? null : trimToNull(request.closingNotes()));
        return toSessionResponse(posSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public PosDtos.PosCatalogLookupResponse lookup(Long sessionId, String query, Long customerId) {
        PosSession session = requireSession(sessionId);
        ProductScanResponse scanResponse = productService.scan(session.getOrganizationId(), session.getWarehouseId(), query);
        StoreProduct storeProduct = productService.get(scanResponse.storeProduct().id());
        if (!Boolean.TRUE.equals(storeProduct.getIsActive())) {
            throw new BusinessException("Store product is inactive: " + storeProduct.getSku());
        }

        LookupPricing lookupPricing = resolveLookupPricing(session, storeProduct, scanResponse, customerId);
        List<PosDtos.PosSellableLotResponse> lots = buildSellableLots(session, storeProduct, scanResponse.batch() == null ? null : scanResponse.batch().id());
        boolean lotSelectionRecommended = hasMixedLotPricing(lots);
        String pricingWarning = lookupPricing.warning();
        if (lotSelectionRecommended && pricingWarning == null) {
            pricingWarning = "Available stock uses different inward-lot prices. Split the sale by lot before checkout.";
        }

        return new PosDtos.PosCatalogLookupResponse(
                query,
                scanResponse.matchedBy(),
                scanResponse.storeProduct().id(),
                scanResponse.storeProduct().productId(),
                scanResponse.storeProduct().sku(),
                scanResponse.storeProduct().name(),
                scanResponse.storeProduct().description(),
                scanResponse.storeProduct().baseUomId(),
                scanResponse.storeProduct().taxGroupId(),
                scanResponse.product() == null ? null : scanResponse.product().hsnCode(),
                scanResponse.storeProduct().inventoryTrackingMode(),
                scanResponse.storeProduct().serialTrackingEnabled(),
                scanResponse.storeProduct().batchTrackingEnabled(),
                scanResponse.storeProduct().fractionalQuantityAllowed(),
                scanResponse.storeProduct().isServiceItem(),
                scanResponse.storeProduct().isActive(),
                lookupPricing.unitPrice(),
                lookupPricing.mrp(),
                scanResponse.stock() == null ? BigDecimal.ZERO : zeroIfNull(scanResponse.stock().onHandBaseQuantity()),
                scanResponse.stock() == null ? BigDecimal.ZERO : zeroIfNull(scanResponse.stock().reservedBaseQuantity()),
                scanResponse.stock() == null ? BigDecimal.ZERO : zeroIfNull(scanResponse.stock().availableBaseQuantity()),
                lotSelectionRecommended,
                lookupPricing.source(),
                pricingWarning,
                scanResponse.serial() == null ? null : scanResponse.serial().id(),
                scanResponse.serial() == null ? null : scanResponse.serial().serialNumber(),
                scanResponse.batch() == null ? null : scanResponse.batch().id(),
                scanResponse.batch() == null ? null : scanResponse.batch().batchNumber(),
                lots
        );
    }

    public PosDtos.PosCheckoutResponse checkout(Long sessionId, PosDtos.PosCheckoutRequest request) {
        PosSession session = requireOpenSession(sessionId);
        Customer customer = resolveCheckoutCustomer(session, request);

        ErpSalesDtos.CreateSalesInvoiceRequest invoiceRequest = new ErpSalesDtos.CreateSalesInvoiceRequest(
                session.getOrganizationId(),
                session.getBranchId(),
                session.getWarehouseId(),
                customer.getId(),
                null,
                request.invoiceDate(),
                request.invoiceDate(),
                customer.getStateCode(),
                trimToNull(request.remarks()),
                request.lines().stream().map(this::toInvoiceLineRequest).toList()
        );

        ErpSalesResponses.SalesInvoiceResponse invoiceResponse = erpSalesService.createInvoice(
                session.getOrganizationId(),
                session.getBranchId(),
                invoiceRequest
        );
        Long invoiceId = invoiceResponse.id();
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + invoiceId));
        invoice.setPosSessionId(sessionId);
        salesInvoiceRepository.save(invoice);

        ErpSalesResponses.CustomerReceiptResponse receiptResponse = null;
        if (request.payment() != null) {
            BigDecimal paymentAmount = request.payment().amount() == null
                    ? invoiceResponse.totalAmount()
                    : request.payment().amount().setScale(2, RoundingMode.HALF_UP);
            if (paymentAmount.compareTo(invoiceResponse.totalAmount()) > 0) {
                throw new BusinessException("POS payment amount cannot exceed invoice total");
            }
            CustomerReceipt receipt = erpSalesService.createReceipt(
                    session.getOrganizationId(),
                    session.getBranchId(),
                    new ErpSalesDtos.CreateCustomerReceiptRequest(
                            session.getOrganizationId(),
                            session.getBranchId(),
                            customer.getId(),
                            request.invoiceDate(),
                            request.payment().paymentMethod(),
                            trimToNull(request.payment().referenceNumber()),
                            paymentAmount,
                            trimToNull(request.payment().remarks())
                    )
            );
            receipt.setPosSessionId(sessionId);
            receipt = customerReceiptRepository.save(receipt);

            if (request.payment().autoAllocate() == null || Boolean.TRUE.equals(request.payment().autoAllocate())) {
                erpSalesService.allocateReceipt(
                        receipt.getId(),
                        new ErpSalesDtos.AllocateReceiptRequest(
                                List.of(new ErpSalesDtos.ReceiptAllocationLine(invoiceResponse.id(), paymentAmount))
                        )
                );
                invoiceResponse = erpSalesService.getInvoice(invoiceResponse.id());
                Long receiptId = receipt.getId();
                receipt = customerReceiptRepository.findById(receiptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer receipt not found: " + receiptId));
            }

            receiptResponse = toReceiptResponse(receipt);
        } else {
            invoiceResponse = erpSalesService.getInvoice(invoiceResponse.id());
        }

        return new PosDtos.PosCheckoutResponse(
                toSessionResponse(session),
                customer.getId(),
                customer.getCustomerCode(),
                customer.getFullName(),
                invoiceResponse,
                receiptResponse
        );
    }

    private PosDtos.PosSessionResponse toSessionResponse(PosSession session) {
        List<SalesInvoice> invoices = salesInvoiceRepository
                .findByOrganizationIdAndPosSessionIdOrderByInvoiceDateDescIdDesc(session.getOrganizationId(), session.getId());
        List<CustomerReceipt> receipts = customerReceiptRepository
                .findByOrganizationIdAndPosSessionIdOrderByReceiptDateDescIdDesc(session.getOrganizationId(), session.getId());
        PosFinancialSummary summary = calculateFinancialSummary(session, invoices, receipts);
        return new PosDtos.PosSessionResponse(
                session.getId(),
                session.getOrganizationId(),
                session.getBranchId(),
                session.getWarehouseId(),
                session.getSessionNumber(),
                session.getTerminalName(),
                session.getStatus(),
                session.getOpenedByUserId(),
                session.getOpenedByUsername(),
                session.getOpenedAt(),
                session.getOpeningNotes(),
                session.getClosedByUserId(),
                session.getClosedByUsername(),
                session.getClosedAt(),
                session.getClosingNotes(),
                session.getOpeningCashAmount(),
                session.getExpectedClosingCashAmount() == null ? summary.expectedClosingCashAmount() : session.getExpectedClosingCashAmount(),
                session.getCountedClosingCashAmount(),
                session.getCashVarianceAmount(),
                invoices.size(),
                receipts.size(),
                summary.grossSalesAmount(),
                summary.totalCollectedAmount(),
                summary.cashCollectedAmount(),
                summary.upiCollectedAmount(),
                summary.cardCollectedAmount(),
                summary.bankCollectedAmount(),
                summary.otherCollectedAmount(),
                invoices.stream().map(invoice -> new PosDtos.PosInvoiceSummaryResponse(
                        invoice.getId(),
                        invoice.getInvoiceNumber(),
                        invoice.getInvoiceDate(),
                        invoice.getCustomerId(),
                        invoice.getTotalAmount(),
                        invoice.getStatus()
                )).toList(),
                receipts.stream().map(this::toReceiptSummaryResponse).toList()
        );
    }

    private PosDtos.PosSessionSummaryResponse toSessionSummaryResponse(PosSession session) {
        PosFinancialSummary summary = calculateFinancialSummary(session);
        List<SalesInvoice> invoices = salesInvoiceRepository
                .findByOrganizationIdAndPosSessionIdOrderByInvoiceDateDescIdDesc(session.getOrganizationId(), session.getId());
        List<CustomerReceipt> receipts = customerReceiptRepository
                .findByOrganizationIdAndPosSessionIdOrderByReceiptDateDescIdDesc(session.getOrganizationId(), session.getId());
        return new PosDtos.PosSessionSummaryResponse(
                session.getId(),
                session.getOrganizationId(),
                session.getBranchId(),
                session.getWarehouseId(),
                session.getSessionNumber(),
                session.getTerminalName(),
                session.getStatus(),
                session.getOpenedByUserId(),
                session.getOpenedByUsername(),
                session.getOpenedAt(),
                session.getClosedAt(),
                session.getOpeningCashAmount(),
                session.getExpectedClosingCashAmount() == null ? summary.expectedClosingCashAmount() : session.getExpectedClosingCashAmount(),
                session.getCountedClosingCashAmount(),
                session.getCashVarianceAmount(),
                invoices.size(),
                receipts.size(),
                summary.grossSalesAmount(),
                summary.totalCollectedAmount(),
                summary.cashCollectedAmount(),
                summary.upiCollectedAmount(),
                summary.cardCollectedAmount(),
                summary.bankCollectedAmount(),
                summary.otherCollectedAmount()
        );
    }

    private PosFinancialSummary calculateFinancialSummary(PosSession session) {
        List<SalesInvoice> invoices = salesInvoiceRepository
                .findByOrganizationIdAndPosSessionIdOrderByInvoiceDateDescIdDesc(session.getOrganizationId(), session.getId());
        List<CustomerReceipt> receipts = customerReceiptRepository
                .findByOrganizationIdAndPosSessionIdOrderByReceiptDateDescIdDesc(session.getOrganizationId(), session.getId());
        return calculateFinancialSummary(session, invoices, receipts);
    }

    private PosFinancialSummary calculateFinancialSummary(PosSession session, List<SalesInvoice> invoices, List<CustomerReceipt> receipts) {
        BigDecimal grossSalesAmount = invoices.stream()
                .map(SalesInvoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal cashCollectedAmount = BigDecimal.ZERO;
        BigDecimal upiCollectedAmount = BigDecimal.ZERO;
        BigDecimal cardCollectedAmount = BigDecimal.ZERO;
        BigDecimal bankCollectedAmount = BigDecimal.ZERO;
        BigDecimal otherCollectedAmount = BigDecimal.ZERO;

        for (CustomerReceipt receipt : receipts) {
            String normalizedMethod = normalizePaymentMethod(receipt.getPaymentMethod());
            BigDecimal amount = zeroIfNull(receipt.getAmount()).setScale(2, RoundingMode.HALF_UP);
            switch (normalizedMethod) {
                case "CASH" -> cashCollectedAmount = cashCollectedAmount.add(amount);
                case "UPI" -> upiCollectedAmount = upiCollectedAmount.add(amount);
                case "CARD" -> cardCollectedAmount = cardCollectedAmount.add(amount);
                case "BANK_TRANSFER", "BANK", "CHEQUE" -> bankCollectedAmount = bankCollectedAmount.add(amount);
                default -> otherCollectedAmount = otherCollectedAmount.add(amount);
            }
        }

        BigDecimal totalCollectedAmount = cashCollectedAmount
                .add(upiCollectedAmount)
                .add(cardCollectedAmount)
                .add(bankCollectedAmount)
                .add(otherCollectedAmount)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedClosingCashAmount = zeroIfNull(session.getOpeningCashAmount())
                .add(cashCollectedAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return new PosFinancialSummary(
                grossSalesAmount,
                totalCollectedAmount,
                cashCollectedAmount.setScale(2, RoundingMode.HALF_UP),
                upiCollectedAmount.setScale(2, RoundingMode.HALF_UP),
                cardCollectedAmount.setScale(2, RoundingMode.HALF_UP),
                bankCollectedAmount.setScale(2, RoundingMode.HALF_UP),
                otherCollectedAmount.setScale(2, RoundingMode.HALF_UP),
                expectedClosingCashAmount
        );
    }

    private PosDtos.PosCheckoutLineRequest toCheckoutLineRequest(ErpSalesDtos.CreateSalesInvoiceLineRequest line) {
        return new PosDtos.PosCheckoutLineRequest(
                line.productId(),
                line.uomId(),
                line.quantity(),
                line.baseQuantity(),
                line.discountAmount(),
                line.serialNumberIds(),
                line.batchSelections(),
                line.warrantyMonths()
        );
    }

    private ErpSalesDtos.CreateSalesInvoiceLineRequest toInvoiceLineRequest(PosDtos.PosCheckoutLineRequest line) {
        return new ErpSalesDtos.CreateSalesInvoiceLineRequest(
                line.storeProductId(),
                line.uomId(),
                line.quantity(),
                line.baseQuantity(),
                line.discountAmount(),
                line.serialNumberIds(),
                line.batchSelections(),
                line.warrantyMonths()
        );
    }

    private Customer resolveCheckoutCustomer(PosSession session, PosDtos.PosCheckoutRequest request) {
        if (request.customerId() != null) {
            return customerRepository.findByIdAndOrganizationId(request.customerId(), session.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
        }
        if (Boolean.TRUE.equals(request.useWalkInCustomer()) || request.customerId() == null) {
            return resolveOrCreateWalkInCustomer(session);
        }
        throw new BusinessException("Customer is required for POS checkout");
    }

    private Customer resolveOrCreateWalkInCustomer(PosSession session) {
        Organization organization = organizationRepository.findById(session.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + session.getOrganizationId()));
        String customerCode = WALK_IN_CUSTOMER_PREFIX + organization.getCode().trim().toUpperCase(Locale.ROOT);
        return customerRepository.findByOrganizationIdAndCustomerCodeIgnoreCase(session.getOrganizationId(), customerCode)
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setOrganizationId(session.getOrganizationId());
                    customer.setBranchId(session.getBranchId());
                    customer.setCustomerCode(customerCode);
                    customer.setFullName("Walk-In Customer");
                    customer.setCustomerType("INDIVIDUAL");
                    customer.setLegalName("Walk-In Customer");
                    customer.setTradeName("Walk-In Customer");
                    customer.setCreditLimit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    customer.setIsPlatformLinked(false);
                    customer.setStatus(ErpDocumentStatuses.ACTIVE);
                    return customerRepository.save(customer);
                });
    }

    private LookupPricing resolveLookupPricing(PosSession session,
                                               StoreProduct storeProduct,
                                               ProductScanResponse scanResponse,
                                               Long customerId) {
        if (scanResponse.serial() != null) {
            SerialNumber serial = serialNumberRepository.findById(scanResponse.serial().id())
                    .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + scanResponse.serial().id()));
            if (serial.getBatchId() != null) {
                InventoryBatch batch = inventoryBatchRepository.findById(serial.getBatchId())
                        .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + serial.getBatchId()));
                return new LookupPricing(
                        batch.getSuggestedSalePrice() == null ? storeProduct.getDefaultSalePrice() : batch.getSuggestedSalePrice(),
                        batch.getMrp() == null ? storeProduct.getDefaultMrp() : batch.getMrp(),
                        "INWARD_LOT",
                        null
                );
            }
        }
        if (scanResponse.batch() != null) {
            InventoryBatch batch = inventoryBatchRepository.findById(scanResponse.batch().id())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + scanResponse.batch().id()));
            return new LookupPricing(
                    batch.getSuggestedSalePrice() == null ? storeProduct.getDefaultSalePrice() : batch.getSuggestedSalePrice(),
                    batch.getMrp() == null ? storeProduct.getDefaultMrp() : batch.getMrp(),
                    "INWARD_LOT",
                    null
            );
        }
        BigDecimal unitPrice = storeProductPricingService.resolveUnitPrice(
                session.getOrganizationId(),
                storeProduct.getId(),
                customerId,
                BigDecimal.ONE,
                LocalDate.now()
        );
        return new LookupPricing(unitPrice, storeProduct.getDefaultMrp(), "STORE_PRICE", null);
    }

    private PosDtos.PosCatalogSearchItemResponse toPosSearchItem(PosSession session,
                                                                 StoreProduct product,
                                                                 Long customerId,
                                                                 String normalizedQuery) {
        BigDecimal available = availableForWarehouseProduct(session, product);
        BigDecimal unitPrice = storeProductPricingService.resolveUnitPrice(
                session.getOrganizationId(),
                product.getId(),
                customerId,
                BigDecimal.ONE,
                LocalDate.now()
        );
        String sku = product.getSku() == null ? "" : product.getSku();
        String name = product.getName() == null ? "" : product.getName();
        return new PosDtos.PosCatalogSearchItemResponse(
                product.getId(),
                product.getProductId(),
                product.getSku(),
                product.getName(),
                product.getBaseUomId(),
                product.getInventoryTrackingMode(),
                product.getSerialTrackingEnabled(),
                product.getBatchTrackingEnabled(),
                product.getIsServiceItem(),
                unitPrice,
                product.getDefaultMrp(),
                available,
                "STORE_PRICE",
                sku.equalsIgnoreCase(normalizedQuery),
                name.equalsIgnoreCase(normalizedQuery)
        );
    }

    private List<PosDtos.PosSellableLotResponse> buildSellableLots(PosSession session, StoreProduct product, Long selectedBatchId) {
        List<InventoryBalance> balances = inventoryBalanceRepository
                .findByOrganizationIdAndProductIdAndWarehouseId(session.getOrganizationId(), product.getId(), session.getWarehouseId())
                .stream()
                .filter(balance -> balance.getAvailableBaseQuantity() != null && balance.getAvailableBaseQuantity().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(InventoryBalance::getCreatedAt).thenComparing(InventoryBalance::getId))
                .toList();

        Map<Long, PosDtos.PosSellableLotResponse> lots = new LinkedHashMap<>();
        BigDecimal legacyAvailable = BigDecimal.ZERO;
        for (InventoryBalance balance : balances) {
            if (balance.getBatchId() == null) {
                legacyAvailable = legacyAvailable.add(zeroIfNull(balance.getAvailableBaseQuantity()));
                continue;
            }
            InventoryBatch batch = inventoryBatchRepository.findById(balance.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + balance.getBatchId()));
            lots.put(batch.getId(), new PosDtos.PosSellableLotResponse(
                    batch.getId(),
                    batch.getBatchNumber(),
                    batch.getBatchType(),
                    zeroIfNull(balance.getAvailableBaseQuantity()).setScale(2, RoundingMode.HALF_UP),
                    batch.getSuggestedSalePrice(),
                    batch.getMrp(),
                    batch.getExpiryOn(),
                    Objects.equals(selectedBatchId, batch.getId())
            ));
        }
        List<PosDtos.PosSellableLotResponse> responses = new ArrayList<>(lots.values());
        if (legacyAvailable.compareTo(BigDecimal.ZERO) > 0) {
            responses.add(new PosDtos.PosSellableLotResponse(
                    null,
                    "LEGACY-STOCK",
                    "LEGACY_STOCK",
                    legacyAvailable.setScale(2, RoundingMode.HALF_UP),
                    product.getDefaultSalePrice(),
                    product.getDefaultMrp(),
                    null,
                    selectedBatchId == null
            ));
        }
        return responses;
    }

    private boolean isSellableInSession(PosSession session, StoreProduct product) {
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            return false;
        }
        if (Boolean.TRUE.equals(product.getIsServiceItem())) {
            return true;
        }
        return availableForWarehouseProduct(session, product).compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasMixedLotPricing(List<PosDtos.PosSellableLotResponse> lots) {
        Set<String> priceKeys = lots.stream()
                .map(lot -> key(lot.suggestedSalePrice(), lot.mrp()))
                .collect(java.util.stream.Collectors.toSet());
        return priceKeys.size() > 1;
    }

    private Comparator<StoreProduct> posSearchComparator(String normalizedQuery) {
        return Comparator
                .comparing((StoreProduct product) -> !exactSkuMatch(product, normalizedQuery))
                .thenComparing(product -> !exactNameMatch(product, normalizedQuery))
                .thenComparing(product -> !startsWithIgnoreCase(product.getSku(), normalizedQuery))
                .thenComparing(product -> !startsWithIgnoreCase(product.getName(), normalizedQuery))
                .thenComparing(StoreProduct::getName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(StoreProduct::getSku, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(StoreProduct::getId);
    }

    private String key(BigDecimal unitPrice, BigDecimal mrp) {
        return String.valueOf(unitPrice) + "|" + String.valueOf(mrp);
    }

    private boolean exactSkuMatch(StoreProduct product, String normalizedQuery) {
        return normalizeComparable(product.getSku()).equals(normalizedQuery);
    }

    private boolean exactNameMatch(StoreProduct product, String normalizedQuery) {
        return normalizeComparable(product.getName()).equals(normalizedQuery);
    }

    private boolean startsWithIgnoreCase(String value, String normalizedQuery) {
        return normalizeComparable(value).startsWith(normalizedQuery);
    }

    private String normalizeComparable(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private BigDecimal availableForWarehouseProduct(PosSession session, StoreProduct product) {
        return inventoryBalanceRepository
                .findByOrganizationIdAndProductIdAndWarehouseId(session.getOrganizationId(), product.getId(), session.getWarehouseId()).stream()
                .map(InventoryBalance::getAvailableBaseQuantity)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PosSession requireSession(Long sessionId) {
        PosSession session = posSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("POS session not found: " + sessionId));
        accessGuard.assertBranchAccess(session.getOrganizationId(), session.getBranchId());
        return session;
    }

    private PosSession requireOpenSession(Long sessionId) {
        PosSession session = requireSession(sessionId);
        if (!ErpDocumentStatuses.OPEN.equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException("POS session is not open");
        }
        return session;
    }

    private Long requiredOrganizationId(Long requestOrganizationId) {
        return requestOrganizationId != null
                ? requestOrganizationId
                : ErpSecurityUtils.currentOrganizationId()
                        .orElseThrow(() -> new BusinessException("Organization context is required"));
    }

    private Long currentUserId() {
        return ErpSecurityUtils.currentUserId()
                .orElseThrow(() -> new BusinessException("Authenticated ERP user context is required"));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeStatus(String status) {
        return trimToNull(status) == null ? null : trimToNull(status).toUpperCase(Locale.ROOT);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = trimToNull(paymentMethod);
        return normalized == null ? "OTHER" : normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ErpSalesResponses.CustomerReceiptResponse toReceiptResponse(CustomerReceipt receipt) {
        return new ErpSalesResponses.CustomerReceiptResponse(
                receipt.getId(),
                receipt.getOrganizationId(),
                receipt.getBranchId(),
                receipt.getCustomerId(),
                receipt.getReceiptNumber(),
                receipt.getReceiptDate(),
                receipt.getPaymentMethod(),
                receipt.getReferenceNumber(),
                receipt.getAmount(),
                receipt.getStatus(),
                receipt.getRemarks()
        );
    }

    private PosDtos.PosReceiptSummaryResponse toReceiptSummaryResponse(CustomerReceipt receipt) {
        return new PosDtos.PosReceiptSummaryResponse(
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getReceiptDate(),
                receipt.getCustomerId(),
                receipt.getPaymentMethod(),
                receipt.getAmount(),
                receipt.getStatus()
        );
    }

    private record LookupPricing(BigDecimal unitPrice, BigDecimal mrp, String source, String warning) {}

    private record PosFinancialSummary(
            BigDecimal grossSalesAmount,
            BigDecimal totalCollectedAmount,
            BigDecimal cashCollectedAmount,
            BigDecimal upiCollectedAmount,
            BigDecimal cardCollectedAmount,
            BigDecimal bankCollectedAmount,
            BigDecimal otherCollectedAmount,
            BigDecimal expectedClosingCashAmount
    ) {}
}
