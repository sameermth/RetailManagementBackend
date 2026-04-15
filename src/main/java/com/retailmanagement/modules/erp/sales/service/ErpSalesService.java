package com.retailmanagement.modules.erp.sales.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.approval.dto.ErpApprovalDtos;
import com.retailmanagement.modules.erp.approval.service.ErpApprovalService;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.entity.StoreProductBundleComponent;
import com.retailmanagement.modules.erp.catalog.entity.Product;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductBundleComponentRepository;
import com.retailmanagement.modules.erp.catalog.repository.UomRepository;
import com.retailmanagement.modules.erp.catalog.service.ProductGovernanceGuard;
import com.retailmanagement.modules.erp.catalog.service.StoreProductPricingService;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.finance.service.ErpAccountingPostingService;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBatchRepository;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.inventory.service.InventoryReservationService;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.repository.StoreCustomerTermsRepository;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesDtos;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesResponses;
import com.retailmanagement.modules.erp.sales.entity.*;
import com.retailmanagement.modules.erp.sales.repository.*;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import com.retailmanagement.modules.erp.tax.service.GstTaxService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ErpSalesService {
    private static final int DEFAULT_QUOTE_VALIDITY_DAYS = 30;
    private static final int DEFAULT_ORDER_FULFILLMENT_DAYS = 15;


    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SalesInvoiceLineRepository salesInvoiceLineRepository;
    private final SalesLineSerialRepository salesLineSerialRepository;
    private final SalesLineBatchRepository salesLineBatchRepository;
    private final SalesQuoteRepository salesQuoteRepository;
    private final SalesQuoteLineRepository salesQuoteLineRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderLineRepository salesOrderLineRepository;
    private final CustomerReceiptRepository customerReceiptRepository;
    private final CustomerReceiptAllocationRepository customerReceiptAllocationRepository;
    private final ProductOwnershipRepository productOwnershipRepository;
    private final StoreProductRepository productRepository;
    private final StoreProductBundleComponentRepository storeProductBundleComponentRepository;
    private final CustomerRepository customerRepository;
    private final StoreCustomerTermsRepository storeCustomerTermsRepository;
    private final UomRepository uomRepository;
    private final ProductGovernanceGuard productGovernanceGuard;
    private final SerialNumberRepository serialNumberRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryReservationService inventoryReservationService;
    private final AuditEventWriter auditEventWriter;
    private final ErpAccessGuard accessGuard;
    private final SubscriptionAccessService subscriptionAccessService;
    private final GstTaxService gstTaxService;
    private final StoreProductPricingService storeProductPricingService;
    private final ErpAccountingPostingService accountingPostingService;
    private final SalesInvoicePostingService salesInvoicePostingService;
    private final SalesDispatchService salesDispatchService;
    private final SalesInvoicePaymentRequestService salesInvoicePaymentRequestService;
    private final ErpApprovalService erpApprovalService;

    @Transactional(readOnly = true)
    public List<SalesInvoice> listInvoices(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        return salesInvoiceRepository.findTop100ByOrganizationIdOrderByInvoiceDateDescIdDesc(organizationId);
    }

    public List<SalesQuote> listQuotes(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        return salesQuoteRepository.findTop100ByOrganizationIdOrderByQuoteDateDescIdDesc(organizationId).stream()
                .map(quote -> synchronizeQuoteStatus(quote, LocalDate.now()))
                .toList();
    }

    public ErpSalesResponses.SalesQuoteResponse getQuote(Long id) {
        SalesQuote quote = salesQuoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales quote not found: " + id));
        accessGuard.assertOrganizationAccess(quote.getOrganizationId());
        accessGuard.assertBranchAccess(quote.getOrganizationId(), quote.getBranchId());
        subscriptionAccessService.assertFeature(quote.getOrganizationId(), "sales");
        quote = synchronizeQuoteStatus(quote, LocalDate.now());
        return toQuoteResponse(quote, salesQuoteLineRepository.findBySalesQuoteIdOrderByIdAsc(id));
    }

    public List<SalesOrder> listOrders(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        return salesOrderRepository.findTop100ByOrganizationIdOrderByOrderDateDescIdDesc(organizationId).stream()
                .map(order -> synchronizeOrderStatus(order, LocalDate.now()))
                .toList();
    }

    public ErpSalesResponses.SalesOrderResponse getOrder(Long id) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found: " + id));
        accessGuard.assertOrganizationAccess(order.getOrganizationId());
        accessGuard.assertBranchAccess(order.getOrganizationId(), order.getBranchId());
        subscriptionAccessService.assertFeature(order.getOrganizationId(), "sales");
        order = synchronizeOrderStatus(order, LocalDate.now());
        return toOrderResponse(order, salesOrderLineRepository.findBySalesOrderIdOrderByIdAsc(id));
    }

    @Transactional(readOnly = true)
    public ErpSalesResponses.SalesInvoiceResponse getInvoice(Long id) {
        SalesInvoice invoice = salesInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + id));
        accessGuard.assertOrganizationAccess(invoice.getOrganizationId());
        accessGuard.assertBranchAccess(invoice.getOrganizationId(), invoice.getBranchId());
        subscriptionAccessService.assertFeature(invoice.getOrganizationId(), "sales");
        List<SalesInvoiceLine> lines = salesInvoiceLineRepository.findBySalesInvoiceIdOrderByIdAsc(id);
        return toInvoiceResponse(invoice, lines);
    }

    public ErpSalesResponses.SalesDispatchResponse pickDispatch(Long dispatchId, ErpSalesDtos.PickSalesDispatchRequest request) {
        return salesDispatchService.pickDispatch(dispatchId, request);
    }

    public ErpSalesResponses.SalesDispatchResponse packDispatch(Long dispatchId, ErpSalesDtos.PackSalesDispatchRequest request) {
        return salesDispatchService.packDispatch(dispatchId, request);
    }

    public ErpSalesResponses.SalesInvoiceResponse createInvoice(Long organizationId, Long branchId, ErpSalesDtos.CreateSalesInvoiceRequest request) {
        return createInvoiceInternal(organizationId, branchId, request, true);
    }

    public ErpSalesResponses.SalesInvoiceResponse createInvoiceSystemGenerated(Long organizationId, Long branchId, ErpSalesDtos.CreateSalesInvoiceRequest request) {
        return createInvoiceInternal(organizationId, branchId, request, false);
    }

    private ErpSalesResponses.SalesInvoiceResponse createInvoiceInternal(Long organizationId, Long branchId,
                                                                         ErpSalesDtos.CreateSalesInvoiceRequest request,
                                                                         boolean enforceUserContext) {
        if (enforceUserContext) {
            accessGuard.assertBranchAccess(organizationId, branchId);
        }
        subscriptionAccessService.assertFeature(organizationId, "sales");
        Customer customer = customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        String invoiceNumber = "INV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
        SalesInvoice invoice = new SalesInvoice();
        invoice.setOrganizationId(organizationId);
        invoice.setBranchId(branchId);
        invoice.setWarehouseId(request.warehouseId());
        invoice.setCustomerId(request.customerId());
        invoice.setPriceListId(request.priceListId());
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(request.invoiceDate() == null ? LocalDate.now() : request.invoiceDate());
        invoice.setDueDate(request.dueDate() == null ? invoice.getInvoiceDate() : request.dueDate());
        invoice.setCustomerGstin(customer.getGstin());
        invoice.setPlaceOfSupplyStateCode(request.placeOfSupplyStateCode());
        invoice.setStatus(ErpDocumentStatuses.SUBMITTED);
        invoice.setRemarks(request.remarks());
        invoice.setSubtotal(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice = salesInvoiceRepository.save(invoice);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        List<ErpSalesDtos.CreateSalesInvoiceLineRequest> expandedLines = expandInvoiceLines(
                organizationId,
                customer.getId(),
                invoice.getInvoiceDate(),
                request.lines()
        );

        for (ErpSalesDtos.CreateSalesInvoiceLineRequest reqLine : expandedLines) {
            StoreProduct product = productRepository.findById(reqLine.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + reqLine.productId()));
            Product productMaster = productGovernanceGuard.requireTransactionAllowed(product, "sales transactions");
            uomRepository.findById(reqLine.uomId())
                    .orElseThrow(() -> new ResourceNotFoundException("UOM not found: " + reqLine.uomId()));
            validateInvoiceTracking(product, reqLine);

            ResolvedInventoryPricing resolvedPricing = resolveInvoiceLinePricing(
                    organizationId,
                    branchId,
                    invoice.getWarehouseId(),
                    request.customerId(),
                    invoice.getInvoiceDate(),
                    customer,
                    product,
                    reqLine
            );
            BigDecimal resolvedUnitPrice = resolvedPricing.unitPrice();
            BigDecimal lineDiscount = reqLine.discountAmount() == null ? BigDecimal.ZERO : reqLine.discountAmount();
            if (lineDiscount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Discount cannot be negative for product " + product.getSku());
            }
            BigDecimal lineBase = resolvedUnitPrice.multiply(reqLine.quantity());
            if (lineDiscount.compareTo(lineBase) > 0) {
                throw new BusinessException("Discount cannot exceed line subtotal for product " + product.getSku());
            }
            BigDecimal lineTaxable = lineBase.subtract(lineDiscount);
            GstTaxService.TaxContext taxContext = gstTaxService.resolveSalesTax(
                    organizationId,
                    branchId,
                    invoice.getInvoiceDate(),
                    productMaster.getHsnCode(),
                    product.getTaxGroupId(),
                    customer.getGstin(),
                    request.placeOfSupplyStateCode(),
                    lineTaxable
            );
            BigDecimal lineTax = taxContext.totalTaxAmount();
            BigDecimal lineTotal = taxContext.lineTotal();
            validateMrpCeiling(product, resolvedPricing.mrp(), reqLine.quantity(), lineTotal);
            BigDecimal lineEstimatedCost = accountingPostingService.estimateSalesCost(
                    organizationId,
                    invoice.getWarehouseId(),
                    reqLine.productId(),
                    reqLine.baseQuantity()
            );

            if (invoice.getSellerTaxRegistrationId() == null) {
                invoice.setSellerTaxRegistrationId(taxContext.sellerTaxRegistrationId());
                invoice.setSellerGstin(taxContext.sellerGstin());
                invoice.setPlaceOfSupplyStateCode(taxContext.placeOfSupplyStateCode());
            }

            SalesInvoiceLine line = new SalesInvoiceLine();
            line.setSalesInvoiceId(invoice.getId());
            line.setProductId(reqLine.productId());
            line.setUomId(reqLine.uomId());
            line.setHsnSnapshot(productMaster.getHsnCode());
            line.setQuantity(reqLine.quantity());
            line.setBaseQuantity(reqLine.baseQuantity());
            line.setUnitPrice(resolvedUnitPrice);
            line.setMrp(resolvedPricing.mrp());
            line.setDiscountAmount(lineDiscount);
            line.setTaxRate(taxContext.effectiveTaxRate());
            line.setTaxableAmount(taxContext.taxableAmount());
            line.setCgstRate(taxContext.cgstRate());
            line.setCgstAmount(taxContext.cgstAmount());
            line.setSgstRate(taxContext.sgstRate());
            line.setSgstAmount(taxContext.sgstAmount());
            line.setIgstRate(taxContext.igstRate());
            line.setIgstAmount(taxContext.igstAmount());
            line.setCessRate(taxContext.cessRate());
            line.setCessAmount(taxContext.cessAmount());
            line.setLineAmount(lineTotal);
            line.setWarrantyMonths(reqLine.warrantyMonths() != null ? reqLine.warrantyMonths() : product.getDefaultWarrantyMonths());
            line.setTotalCostAtSale(lineEstimatedCost);
            line.setUnitCostAtSale(reqLine.baseQuantity().compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : lineEstimatedCost.divide(reqLine.baseQuantity(), 2, RoundingMode.HALF_UP));
            line = salesInvoiceLineRepository.save(line);

            if (reqLine.serialNumberIds() != null && !reqLine.serialNumberIds().isEmpty()) {
                if (reqLine.serialNumberIds().size() != reqLine.baseQuantity().intValue()) {
                    throw new BusinessException("Serialized sales require serial count equal to base quantity for product " + reqLine.productId());
                }
                for (Long serialId : reqLine.serialNumberIds()) {
                    SerialNumber serial = serialNumberRepository.findById(serialId)
                            .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialId));
                    if (!organizationId.equals(serial.getOrganizationId())) {
                        throw new BusinessException("Serial " + serialId + " does not belong to organization " + organizationId);
                    }
                    if (!reqLine.productId().equals(serial.getProductId())) {
                        throw new BusinessException("Serial " + serialId + " does not belong to product " + reqLine.productId());
                    }
                    if (!ErpDocumentStatuses.IN_STOCK.equals(serial.getStatus())) {
                        throw new BusinessException("Serial " + serial.getSerialNumber() + " is not available for sale");
                    }
                    if (serial.getCurrentWarehouseId() == null || !invoice.getWarehouseId().equals(serial.getCurrentWarehouseId())) {
                        throw new BusinessException("Serial " + serial.getSerialNumber() + " is not in warehouse " + invoice.getWarehouseId());
                    }
                    SalesLineSerial link = new SalesLineSerial();
                    link.setSalesInvoiceLineId(line.getId());
                    link.setSerialNumberId(serialId);
                    salesLineSerialRepository.save(link);
                }
            }

            for (BatchAllocation allocation : resolvedPricing.batchAllocations()) {
                SalesLineBatch link = new SalesLineBatch();
                link.setSalesInvoiceLineId(line.getId());
                link.setBatchId(allocation.batchId());
                link.setQuantity(allocation.quantity());
                link.setBaseQuantity(allocation.baseQuantity());
                salesLineBatchRepository.save(link);
            }

            subtotal = subtotal.add(lineBase);
            discount = discount.add(lineDiscount);
            taxAmount = taxAmount.add(lineTax);
            total = total.add(lineTotal);
        }

        invoice.setSubtotal(subtotal);
        invoice.setDiscountAmount(discount);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(total);
        invoice = salesInvoiceRepository.save(invoice);
        inventoryReservationService.reserveSalesInvoice(invoice);
        ErpApprovalService.ApprovalEvaluation evaluation = erpApprovalService.evaluate(
                organizationId,
                new ErpApprovalDtos.ApprovalEvaluationQuery("sales_invoice", invoice.getId(), "SALES_INVOICE_CREATE")
        );
        if (evaluation.approvalRequired()) {
            invoice.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
            invoice = salesInvoiceRepository.save(invoice);
            if (!evaluation.pendingRequestExists()) {
                erpApprovalService.createRequest(
                        organizationId,
                        branchId,
                        new ErpApprovalDtos.CreateApprovalRequest(
                                "sales_invoice",
                                invoice.getId(),
                                invoice.getInvoiceNumber(),
                                "SALES_INVOICE_CREATE",
                                "Sales invoice amount matched approval rule",
                                null,
                                null
                        )
                );
            }
            auditEventWriter.write(
                    organizationId,
                    branchId,
                    "SALES_INVOICE_SUBMITTED",
                    "sales_invoice",
                    invoice.getId(),
                    invoice.getInvoiceNumber(),
                    "SUBMIT",
                    invoice.getWarehouseId(),
                    invoice.getCustomerId(),
                    null,
                    "Sales invoice submitted for approval",
                    ErpJsonPayloads.of("invoiceNumber", invoice.getInvoiceNumber(), "invoiceId", invoice.getId(), "total", invoice.getTotalAmount())
            );
        } else {
            invoice = salesInvoicePostingService.finalizeApprovedInvoice(invoice.getId());
        }

        List<SalesInvoiceLine> savedLines = salesInvoiceLineRepository.findBySalesInvoiceIdOrderByIdAsc(invoice.getId());
        return toInvoiceResponse(invoice, savedLines);
    }

    public ErpSalesResponses.SalesQuoteResponse createQuote(Long organizationId, Long branchId, ErpSalesDtos.CreateSalesQuoteRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        validateQuoteType(request.quoteType());
        Customer customer = customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        String quoteNumber = quoteNumber(request.quoteType());
        SalesQuote quote = new SalesQuote();
        quote.setOrganizationId(organizationId);
        quote.setBranchId(branchId);
        quote.setWarehouseId(request.warehouseId());
        quote.setCustomerId(request.customerId());
        quote.setQuoteType(request.quoteType().trim().toUpperCase());
        quote.setQuoteNumber(quoteNumber);
        LocalDate quoteDate = request.quoteDate() == null ? LocalDate.now() : request.quoteDate();
        quote.setQuoteDate(quoteDate);
        quote.setValidUntil(resolveQuoteValidUntil(quoteDate, request.validUntil()));
        quote.setCustomerGstin(customer.getGstin());
        quote.setPlaceOfSupplyStateCode(request.placeOfSupplyStateCode());
        quote.setStatus(ErpDocumentStatuses.DRAFT);
        quote.setRemarks(request.remarks());
        quote = salesQuoteRepository.save(quote);

        Totals totals = saveQuoteLines(quote, customer, request.lines());
        quote.setSellerTaxRegistrationId(totals.sellerTaxRegistrationId());
        quote.setSellerGstin(totals.sellerGstin());
        quote.setPlaceOfSupplyStateCode(totals.placeOfSupplyStateCode());
        quote.setSubtotal(totals.subtotal());
        quote.setDiscountAmount(totals.discount());
        quote.setTaxAmount(totals.taxAmount());
        quote.setTotalAmount(totals.total());
        quote.setStatus(ErpDocumentStatuses.SUBMITTED);
        quote = salesQuoteRepository.save(quote);

        return toQuoteResponse(quote, salesQuoteLineRepository.findBySalesQuoteIdOrderByIdAsc(quote.getId()));
    }

    public ErpSalesResponses.SalesOrderResponse createOrder(Long organizationId, Long branchId, ErpSalesDtos.CreateSalesOrderRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        Customer customer = customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        SalesOrder order = new SalesOrder();
        order.setOrganizationId(organizationId);
        order.setBranchId(branchId);
        order.setWarehouseId(request.warehouseId());
        order.setCustomerId(request.customerId());
        order.setOrderNumber("SO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis());
        LocalDate orderDate = request.orderDate() == null ? LocalDate.now() : request.orderDate();
        order.setOrderDate(orderDate);
        order.setExpectedFulfillmentBy(resolveOrderFulfillmentBy(orderDate, request.expectedFulfillmentBy()));
        order.setCustomerGstin(customer.getGstin());
        order.setPlaceOfSupplyStateCode(request.placeOfSupplyStateCode());
        order.setStatus(ErpDocumentStatuses.SUBMITTED);
        order.setRemarks(request.remarks());
        order = salesOrderRepository.save(order);

        Totals totals = saveOrderLines(order, customer, request.lines(), null);
        order.setSellerTaxRegistrationId(totals.sellerTaxRegistrationId());
        order.setSellerGstin(totals.sellerGstin());
        order.setPlaceOfSupplyStateCode(totals.placeOfSupplyStateCode());
        order.setSubtotal(totals.subtotal());
        order.setDiscountAmount(totals.discount());
        order.setTaxAmount(totals.taxAmount());
        order.setTotalAmount(totals.total());
        order = salesOrderRepository.save(order);

        return toOrderResponse(order, salesOrderLineRepository.findBySalesOrderIdOrderByIdAsc(order.getId()));
    }

    public ErpSalesResponses.SalesOrderResponse convertQuoteToOrder(Long quoteId, ErpSalesDtos.ConvertSalesQuoteRequest request) {
        SalesQuote quote = salesQuoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales quote not found: " + quoteId));
        accessGuard.assertBranchAccess(quote.getOrganizationId(), quote.getBranchId());
        subscriptionAccessService.assertFeature(quote.getOrganizationId(), "sales");
        ensureQuoteAvailableForConversion(quote, request.targetDate() == null ? LocalDate.now() : request.targetDate());
        if (quote.getConvertedSalesOrderId() != null) {
            return getOrder(quote.getConvertedSalesOrderId());
        }
        List<SalesQuoteLine> quoteLines = salesQuoteLineRepository.findBySalesQuoteIdOrderByIdAsc(quoteId);
        Customer customer = customerRepository.findByIdAndOrganizationId(quote.getCustomerId(), quote.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + quote.getCustomerId()));

        SalesOrder order = new SalesOrder();
        order.setOrganizationId(quote.getOrganizationId());
        order.setBranchId(request.branchId() != null ? request.branchId() : quote.getBranchId());
        order.setWarehouseId(quote.getWarehouseId());
        order.setCustomerId(quote.getCustomerId());
        order.setSourceQuoteId(quote.getId());
        order.setOrderNumber("SO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis());
        LocalDate orderDate = request.targetDate() == null ? LocalDate.now() : request.targetDate();
        order.setOrderDate(orderDate);
        order.setExpectedFulfillmentBy(resolveOrderFulfillmentBy(orderDate, null));
        order.setCustomerGstin(quote.getCustomerGstin());
        order.setPlaceOfSupplyStateCode(quote.getPlaceOfSupplyStateCode());
        order.setStatus(ErpDocumentStatuses.SUBMITTED);
        order.setRemarks(request.remarks() == null ? "Converted from " + quote.getQuoteNumber() : request.remarks());
        order = salesOrderRepository.save(order);

        Totals totals = saveOrderLines(order, customer, toDocumentLineRequests(quoteLines), quoteLines);
        order.setSellerTaxRegistrationId(totals.sellerTaxRegistrationId());
        order.setSellerGstin(totals.sellerGstin());
        order.setSubtotal(totals.subtotal());
        order.setDiscountAmount(totals.discount());
        order.setTaxAmount(totals.taxAmount());
        order.setTotalAmount(totals.total());
        order = salesOrderRepository.save(order);

        quote.setConvertedSalesOrderId(order.getId());
        quote.setStatus("ORDERED");
        salesQuoteRepository.saveAndFlush(quote);
        return toOrderResponse(order, salesOrderLineRepository.findBySalesOrderIdOrderByIdAsc(order.getId()));
    }

    public ErpSalesResponses.SalesInvoiceResponse convertQuoteToInvoice(Long quoteId, ErpSalesDtos.ConvertSalesQuoteRequest request) {
        SalesQuote quote = salesQuoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales quote not found: " + quoteId));
        accessGuard.assertBranchAccess(quote.getOrganizationId(), quote.getBranchId());
        subscriptionAccessService.assertFeature(quote.getOrganizationId(), "sales");
        ensureQuoteAvailableForConversion(quote, request.targetDate() == null ? LocalDate.now() : request.targetDate());
        if (quote.getConvertedSalesInvoiceId() != null) {
            return getInvoice(quote.getConvertedSalesInvoiceId());
        }

        ErpSalesDtos.CreateSalesInvoiceRequest invoiceRequest = new ErpSalesDtos.CreateSalesInvoiceRequest(
                request.organizationId() != null ? request.organizationId() : quote.getOrganizationId(),
                request.branchId() != null ? request.branchId() : quote.getBranchId(),
                quote.getWarehouseId(),
                quote.getCustomerId(),
                null,
                request.targetDate() == null ? LocalDate.now() : request.targetDate(),
                request.targetDate() == null ? LocalDate.now() : request.targetDate(),
                quote.getPlaceOfSupplyStateCode(),
                request.remarks() == null ? "Converted from " + quote.getQuoteNumber() : request.remarks(),
                toInvoiceLineRequests(
                        salesQuoteLineRepository.findBySalesQuoteIdOrderByIdAsc(quoteId),
                        quote.getQuoteNumber(),
                        trackedSelectionsByProduct(request.trackedLines())
                )
        );
        ErpSalesResponses.SalesInvoiceResponse response = createInvoice(quote.getOrganizationId(), invoiceRequest.branchId(), invoiceRequest);
        quote.setConvertedSalesInvoiceId(response.id());
        quote.setStatus("INVOICED");
        salesQuoteRepository.saveAndFlush(quote);
        return response;
    }

    public ErpSalesResponses.SalesInvoiceResponse convertOrderToInvoice(Long orderId, ErpSalesDtos.ConvertSalesOrderRequest request) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found: " + orderId));
        accessGuard.assertBranchAccess(order.getOrganizationId(), order.getBranchId());
        subscriptionAccessService.assertFeature(order.getOrganizationId(), "sales");
        ensureOrderAvailableForInvoicing(order, request.targetDate() == null ? LocalDate.now() : request.targetDate());
        if (order.getConvertedSalesInvoiceId() != null) {
            return getInvoice(order.getConvertedSalesInvoiceId());
        }

        ErpSalesDtos.CreateSalesInvoiceRequest invoiceRequest = new ErpSalesDtos.CreateSalesInvoiceRequest(
                request.organizationId() != null ? request.organizationId() : order.getOrganizationId(),
                request.branchId() != null ? request.branchId() : order.getBranchId(),
                order.getWarehouseId(),
                order.getCustomerId(),
                null,
                request.targetDate() == null ? LocalDate.now() : request.targetDate(),
                request.targetDate() == null ? LocalDate.now() : request.targetDate(),
                order.getPlaceOfSupplyStateCode(),
                request.remarks() == null ? "Converted from " + order.getOrderNumber() : request.remarks(),
                toInvoiceLineRequestsFromOrder(
                        salesOrderLineRepository.findBySalesOrderIdOrderByIdAsc(orderId),
                        order.getOrderNumber(),
                        trackedSelectionsByProduct(request.trackedLines())
                )
        );
        ErpSalesResponses.SalesInvoiceResponse response = createInvoice(order.getOrganizationId(), invoiceRequest.branchId(), invoiceRequest);
        order.setConvertedSalesInvoiceId(response.id());
        order.setStatus("INVOICED");
        salesOrderRepository.saveAndFlush(order);
        if (order.getSourceQuoteId() != null) {
            salesQuoteRepository.findById(order.getSourceQuoteId()).ifPresent(quote -> {
                quote.setConvertedSalesInvoiceId(response.id());
                quote.setStatus("INVOICED");
                salesQuoteRepository.saveAndFlush(quote);
            });
        }
        return response;
    }

    public ErpSalesResponses.SalesQuoteResponse cancelQuote(Long quoteId, ErpSalesDtos.CancelSalesDocumentRequest request) {
        SalesQuote quote = salesQuoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales quote not found: " + quoteId));
        Long organizationId = quote.getOrganizationId();
        Long branchId = request.branchId() != null ? request.branchId() : quote.getBranchId();
        accessGuard.assertBranchAccess(organizationId, quote.getBranchId());
        subscriptionAccessService.assertFeature(organizationId, "sales");
        if (ErpDocumentStatuses.CANCELLED.equals(quote.getStatus())) {
            return toQuoteResponse(quote, salesQuoteLineRepository.findBySalesQuoteIdOrderByIdAsc(quoteId));
        }
        if (quote.getConvertedSalesInvoiceId() != null) {
            throw new BusinessException("Cannot cancel a quote that has already been converted to an invoice");
        }
        if (quote.getConvertedSalesOrderId() != null) {
            final Long convertedSalesOrderId = quote.getConvertedSalesOrderId();
            SalesOrder convertedOrder = salesOrderRepository.findById(convertedSalesOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sales order not found: " + convertedSalesOrderId));
            if (!ErpDocumentStatuses.CANCELLED.equals(convertedOrder.getStatus())) {
                throw new BusinessException("Cannot cancel a quote that has already been converted to an active sales order");
            }
        }
        String reason = request.reason().trim();
        quote.setStatus(ErpDocumentStatuses.CANCELLED);
        quote.setRemarks(appendCancellationReason(quote.getRemarks(), reason));
        quote = salesQuoteRepository.save(quote);
        auditEventWriter.write(
                organizationId,
                branchId,
                "SALES_QUOTE_CANCELLED",
                "sales_quote",
                quote.getId(),
                quote.getQuoteNumber(),
                "CANCEL",
                quote.getWarehouseId(),
                quote.getCustomerId(),
                null,
                "Sales quote cancelled",
                ErpJsonPayloads.of("reason", reason)
        );
        return toQuoteResponse(quote, salesQuoteLineRepository.findBySalesQuoteIdOrderByIdAsc(quoteId));
    }

    public ErpSalesResponses.SalesOrderResponse cancelOrder(Long orderId, ErpSalesDtos.CancelSalesDocumentRequest request) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found: " + orderId));
        Long organizationId = order.getOrganizationId();
        Long branchId = request.branchId() != null ? request.branchId() : order.getBranchId();
        accessGuard.assertBranchAccess(organizationId, order.getBranchId());
        subscriptionAccessService.assertFeature(organizationId, "sales");
        if (ErpDocumentStatuses.CANCELLED.equals(order.getStatus())) {
            return toOrderResponse(order, salesOrderLineRepository.findBySalesOrderIdOrderByIdAsc(orderId));
        }
        if (order.getConvertedSalesInvoiceId() != null) {
            throw new BusinessException("Cannot cancel a sales order that has already been converted to an invoice");
        }
        String reason = request.reason().trim();
        order.setStatus(ErpDocumentStatuses.CANCELLED);
        order.setRemarks(appendCancellationReason(order.getRemarks(), reason));
        order = salesOrderRepository.save(order);

        if (order.getSourceQuoteId() != null) {
            final Long cancelledOrderId = order.getId();
            salesQuoteRepository.findById(order.getSourceQuoteId()).ifPresent(sourceQuote -> {
                if (!ErpDocumentStatuses.CANCELLED.equals(sourceQuote.getStatus())
                        && sourceQuote.getConvertedSalesInvoiceId() == null
                        && cancelledOrderId.equals(sourceQuote.getConvertedSalesOrderId())) {
                    sourceQuote.setStatus(ErpDocumentStatuses.SUBMITTED);
                    salesQuoteRepository.save(sourceQuote);
                }
            });
        }

        auditEventWriter.write(
                organizationId,
                branchId,
                "SALES_ORDER_CANCELLED",
                "sales_order",
                order.getId(),
                order.getOrderNumber(),
                "CANCEL",
                order.getWarehouseId(),
                order.getCustomerId(),
                null,
                "Sales order cancelled",
                ErpJsonPayloads.of("reason", reason)
        );
        return toOrderResponse(order, salesOrderLineRepository.findBySalesOrderIdOrderByIdAsc(orderId));
    }

    @Transactional(readOnly = true)
    public List<CustomerReceipt> listReceipts(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "payments");
        return customerReceiptRepository.findTop100ByOrganizationIdOrderByReceiptDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public CustomerReceipt getReceipt(Long receiptId) {
        CustomerReceipt receipt = customerReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer receipt not found: " + receiptId));
        accessGuard.assertOrganizationAccess(receipt.getOrganizationId());
        subscriptionAccessService.assertFeature(receipt.getOrganizationId(), "payments");
        return receipt;
    }

    public CustomerReceipt createReceipt(Long organizationId, Long branchId, ErpSalesDtos.CreateCustomerReceiptRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "payments");
        customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        String receiptNumber = "RCT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
        CustomerReceipt receipt = new CustomerReceipt();
        receipt.setOrganizationId(organizationId);
        receipt.setBranchId(branchId);
        receipt.setCustomerId(request.customerId());
        receipt.setReceiptNumber(receiptNumber);
        receipt.setReceiptDate(request.receiptDate() == null ? LocalDate.now() : request.receiptDate());
        receipt.setPaymentMethod(request.paymentMethod().toUpperCase());
        receipt.setReferenceNumber(request.referenceNumber());
        receipt.setAmount(request.amount());
        receipt.setStatus(ErpDocumentStatuses.POSTED);
        receipt.setRemarks(request.remarks());
        receipt = customerReceiptRepository.save(receipt);
        accountingPostingService.postCustomerReceipt(receipt);
        salesInvoicePaymentRequestService.synchronizeInvoiceRequestsForCustomer(request.customerId(), organizationId);

        auditEventWriter.write(
                organizationId,
                branchId,
                "CUSTOMER_RECEIPT_POSTED",
                "customer_receipt",
                receipt.getId(),
                receipt.getReceiptNumber(),
                "POST",
                null,
                receipt.getCustomerId(),
                null,
                "Customer receipt posted",
                ErpJsonPayloads.of(
                        "receiptNumber", receipt.getReceiptNumber(),
                        "amount", receipt.getAmount()
                )
        );

        return receipt;
    }

    private void validateInvoiceTracking(StoreProduct product, ErpSalesDtos.CreateSalesInvoiceLineRequest reqLine) {
        boolean expectsSerials = Boolean.TRUE.equals(product.getSerialTrackingEnabled());
        boolean expectsBatches = Boolean.TRUE.equals(product.getBatchTrackingEnabled());
        boolean hasSerials = reqLine.serialNumberIds() != null && !reqLine.serialNumberIds().isEmpty();
        boolean hasBatches = reqLine.batchSelections() != null && !reqLine.batchSelections().isEmpty();

        if (expectsSerials) {
            if (!hasSerials) {
                throw new BusinessException("Serialized product requires serial numbers on sale: " + product.getSku());
            }
            if (hasBatches) {
                throw new BusinessException("Serialized product cannot use batch selections: " + product.getSku());
            }
            if (reqLine.baseQuantity().stripTrailingZeros().scale() > 0) {
                throw new BusinessException("Serialized product base quantity must be a whole number: " + product.getSku());
            }
            return;
        }

        if (expectsBatches) {
            if (!hasBatches) {
                throw new BusinessException("Batch-tracked product requires batch selections on sale: " + product.getSku());
            }
            if (hasSerials) {
                throw new BusinessException("Batch-tracked product cannot use serial numbers: " + product.getSku());
            }
            return;
        }

        if (hasSerials) {
            throw new BusinessException("Standard product does not accept serial or batch details: " + product.getSku());
        }
    }

    private ResolvedInventoryPricing resolveInvoiceLinePricing(Long organizationId,
                                                               Long branchId,
                                                               Long warehouseId,
                                                               Long customerId,
                                                               LocalDate invoiceDate,
                                                               Customer customer,
                                                               StoreProduct product,
                                                               ErpSalesDtos.CreateSalesInvoiceLineRequest reqLine) {
        if (reqLine.serialNumberIds() != null && !reqLine.serialNumberIds().isEmpty()) {
            return resolveSerializedPricing(organizationId, warehouseId, customerId, invoiceDate, product, reqLine);
        }
        if (reqLine.batchSelections() != null && !reqLine.batchSelections().isEmpty()) {
            return resolveBatchSelectionPricing(organizationId, warehouseId, customerId, invoiceDate, product, reqLine);
        }
        return resolveStandardPricing(organizationId, branchId, warehouseId, customerId, invoiceDate, customer, product, reqLine);
    }

    private ResolvedInventoryPricing resolveSerializedPricing(Long organizationId,
                                                              Long warehouseId,
                                                              Long customerId,
                                                              LocalDate invoiceDate,
                                                              StoreProduct product,
                                                              ErpSalesDtos.CreateSalesInvoiceLineRequest reqLine) {
        List<SerialNumber> serials = reqLine.serialNumberIds().stream()
                .map(serialId -> serialNumberRepository.findById(serialId)
                        .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialId)))
                .toList();
        InventoryBatch pricedBatch = null;
        for (SerialNumber serial : serials) {
            if (!organizationId.equals(serial.getOrganizationId())) {
                throw new BusinessException("Serial " + serial.getId() + " does not belong to organization " + organizationId);
            }
            if (!product.getId().equals(serial.getProductId())) {
                throw new BusinessException("Serial " + serial.getSerialNumber() + " does not belong to product " + product.getSku());
            }
            if (serial.getCurrentWarehouseId() == null || !warehouseId.equals(serial.getCurrentWarehouseId())) {
                throw new BusinessException("Serial " + serial.getSerialNumber() + " is not in warehouse " + warehouseId);
            }
            if (serial.getBatchId() == null) {
                continue;
            }
            InventoryBatch batch = inventoryBatchRepository.findById(serial.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + serial.getBatchId()));
            validateBatchOwnership(product, organizationId, batch);
            if (pricedBatch == null) {
                pricedBatch = batch;
                continue;
            }
            validateConsistentPricing(product, pricedBatch, batch);
        }
        if (pricedBatch == null) {
            return fallbackProductPricing(organizationId, customerId, invoiceDate, product, reqLine);
        }
        return pricedBatchPricing(product, reqLine, List.of(), pricedBatch);
    }

    private ResolvedInventoryPricing resolveBatchSelectionPricing(Long organizationId,
                                                                  Long warehouseId,
                                                                  Long customerId,
                                                                  LocalDate invoiceDate,
                                                                  StoreProduct product,
                                                                  ErpSalesDtos.CreateSalesInvoiceLineRequest reqLine) {
        BigDecimal batchBase = BigDecimal.ZERO;
        InventoryBatch pricedBatch = null;
        List<BatchAllocation> allocations = new ArrayList<>();
        for (ErpSalesDtos.BatchSelection batchSelection : reqLine.batchSelections()) {
            InventoryBatch batch = inventoryBatchRepository.findById(batchSelection.batchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchSelection.batchId()));
            validateBatchOwnership(product, organizationId, batch);
            ensureBatchAvailability(organizationId, warehouseId, product.getId(), batch.getId(), batchSelection.baseQuantity());
            if (pricedBatch == null) {
                pricedBatch = batch;
            } else {
                validateConsistentPricing(product, pricedBatch, batch);
            }
            allocations.add(new BatchAllocation(batchSelection.batchId(), batchSelection.quantity(), batchSelection.baseQuantity()));
            batchBase = batchBase.add(batchSelection.baseQuantity());
        }
        if (batchBase.compareTo(reqLine.baseQuantity()) != 0) {
            throw new BusinessException("Batch base quantity mismatch for product " + product.getSku());
        }
        if (pricedBatch == null) {
            return fallbackProductPricing(organizationId, customerId, invoiceDate, product, reqLine);
        }
        return pricedBatchPricing(product, reqLine, allocations, pricedBatch);
    }

    private ResolvedInventoryPricing resolveStandardPricing(Long organizationId,
                                                            Long branchId,
                                                            Long warehouseId,
                                                            Long customerId,
                                                            LocalDate invoiceDate,
                                                            Customer customer,
                                                            StoreProduct product,
                                                            ErpSalesDtos.CreateSalesInvoiceLineRequest reqLine) {
        List<InventoryBalance> balances = inventoryBalanceRepository
                .findByOrganizationIdAndProductIdAndWarehouseId(organizationId, product.getId(), warehouseId).stream()
                .filter(balance -> balance.getAvailableBaseQuantity() != null && balance.getAvailableBaseQuantity().compareTo(BigDecimal.ZERO) > 0)
                .sorted(java.util.Comparator.comparing(InventoryBalance::getCreatedAt).thenComparing(InventoryBalance::getId))
                .toList();

        List<BatchAllocation> allocations = new ArrayList<>();
        InventoryBatch pricedBatch = null;
        BigDecimal remaining = reqLine.baseQuantity();
        BigDecimal qtyPerBaseUnit = reqLine.quantity().divide(reqLine.baseQuantity(), 6, RoundingMode.HALF_UP);
        BigDecimal legacyAvailable = BigDecimal.ZERO;

        for (InventoryBalance balance : balances) {
            if (balance.getBatchId() == null) {
                legacyAvailable = legacyAvailable.add(balance.getAvailableBaseQuantity());
                continue;
            }
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            InventoryBatch batch = inventoryBatchRepository.findById(balance.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + balance.getBatchId()));
            validateBatchOwnership(product, organizationId, batch);
            if (pricedBatch == null) {
                pricedBatch = batch;
            } else {
                validateConsistentPricing(product, pricedBatch, batch);
            }
            BigDecimal allocatedBaseQuantity = balance.getAvailableBaseQuantity().min(remaining);
            BigDecimal allocatedQuantity = allocatedBaseQuantity.multiply(qtyPerBaseUnit).setScale(6, RoundingMode.HALF_UP);
            allocations.add(new BatchAllocation(batch.getId(), allocatedQuantity, allocatedBaseQuantity));
            remaining = remaining.subtract(allocatedBaseQuantity);
        }

        if (remaining.compareTo(BigDecimal.ZERO) <= 0 && pricedBatch != null) {
            return pricedBatchPricing(product, reqLine, allocations, pricedBatch);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && legacyAvailable.compareTo(remaining) >= 0) {
            return fallbackProductPricing(organizationId, customerId, invoiceDate, product, reqLine);
        }
        if (pricedBatch != null && !allocations.isEmpty()) {
            throw new BusinessException("Available stock for product " + product.getSku() + " spans multiple inward lots. Split the sale line to continue.");
        }
        return fallbackProductPricing(organizationId, customerId, invoiceDate, product, reqLine);
    }

    private ResolvedInventoryPricing fallbackProductPricing(Long organizationId,
                                                            Long customerId,
                                                            LocalDate invoiceDate,
                                                            StoreProduct product,
                                                            ErpSalesDtos.CreateSalesInvoiceLineRequest reqLine) {
        BigDecimal unitPrice = storeProductPricingService.resolveUnitPrice(
                organizationId,
                product.getId(),
                customerId,
                reqLine.baseQuantity(),
                invoiceDate
        );
        return new ResolvedInventoryPricing(unitPrice, product.getDefaultMrp(), List.of());
    }

    private ResolvedInventoryPricing pricedBatchPricing(StoreProduct product,
                                                        ErpSalesDtos.CreateSalesInvoiceLineRequest reqLine,
                                                        List<BatchAllocation> allocations,
                                                        InventoryBatch pricedBatch) {
        BigDecimal unitPrice = toSalesUnitPrice(pricedBatch.getSuggestedSalePrice(), reqLine.quantity(), reqLine.baseQuantity());
        if (unitPrice == null) {
            unitPrice = product.getDefaultSalePrice();
        }
        if (unitPrice == null) {
            throw new BusinessException("No selling price is configured for product " + product.getSku());
        }
        BigDecimal mrp = toSalesUnitPrice(pricedBatch.getMrp(), reqLine.quantity(), reqLine.baseQuantity());
        return new ResolvedInventoryPricing(unitPrice, mrp, allocations);
    }

    private BigDecimal toSalesUnitPrice(BigDecimal perBaseUnitPrice, BigDecimal quantity, BigDecimal baseQuantity) {
        if (perBaseUnitPrice == null || quantity == null || baseQuantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return perBaseUnitPrice;
        }
        return perBaseUnitPrice.multiply(baseQuantity).divide(quantity, 2, RoundingMode.HALF_UP);
    }

    private void ensureBatchAvailability(Long organizationId,
                                         Long warehouseId,
                                         Long productId,
                                         Long batchId,
                                         BigDecimal requiredBaseQuantity) {
        InventoryBalance balance = inventoryBalanceRepository.findByOrganizationIdAndProductIdAndWarehouseId(organizationId, productId, warehouseId)
                .stream()
                .filter(candidate -> batchId.equals(candidate.getBatchId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Inventory balance not found for batch " + batchId + " in warehouse " + warehouseId));
        if (balance.getAvailableBaseQuantity() == null || balance.getAvailableBaseQuantity().compareTo(requiredBaseQuantity) < 0) {
            throw new BusinessException("Insufficient available stock in selected batch for product " + productId);
        }
    }

    private void validateBatchOwnership(StoreProduct product, Long organizationId, InventoryBatch batch) {
        if (!organizationId.equals(batch.getOrganizationId())) {
            throw new BusinessException("Batch " + batch.getId() + " does not belong to organization " + organizationId);
        }
        if (!product.getId().equals(batch.getProductId())) {
            throw new BusinessException("Batch " + batch.getId() + " does not belong to product " + product.getSku());
        }
    }

    private void validateConsistentPricing(StoreProduct product, InventoryBatch first, InventoryBatch next) {
        if (!java.util.Objects.equals(first.getSuggestedSalePrice(), next.getSuggestedSalePrice())
                || !java.util.Objects.equals(first.getMrp(), next.getMrp())) {
            throw new BusinessException("Selected stock for product " + product.getSku() + " has different MRP or selling price across inward lots. Split the sale into separate lines.");
        }
    }

    private void validateMrpCeiling(StoreProduct product, BigDecimal unitMrp, BigDecimal quantity, BigDecimal lineTotal) {
        if (unitMrp == null || quantity == null || unitMrp.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal maxLineAmount = unitMrp.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        if (lineTotal.compareTo(maxLineAmount) > 0) {
            throw new BusinessException("Final selling amount cannot exceed MRP for product " + product.getSku());
        }
    }

    public CustomerReceipt allocateReceipt(Long receiptId, ErpSalesDtos.AllocateReceiptRequest request) {
        CustomerReceipt receipt = customerReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer receipt not found: " + receiptId));
        accessGuard.assertOrganizationAccess(receipt.getOrganizationId());
        accessGuard.assertBranchAccess(receipt.getOrganizationId(), receipt.getBranchId());
        subscriptionAccessService.assertFeature(receipt.getOrganizationId(), "payments");
        if (ErpDocumentStatuses.CANCELLED.equals(receipt.getStatus())) {
            throw new BusinessException("Cannot allocate a cancelled receipt");
        }

        BigDecimal totalAllocated = BigDecimal.ZERO;
        for (ErpSalesDtos.ReceiptAllocationLine line : request.allocations()) {
            SalesInvoice invoice = salesInvoiceRepository.findById(line.salesInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + line.salesInvoiceId()));
            if (!receipt.getOrganizationId().equals(invoice.getOrganizationId())) {
                throw new BusinessException("Sales invoice does not belong to the receipt organization");
            }
            CustomerReceiptAllocation allocation = new CustomerReceiptAllocation();
            allocation.setCustomerReceiptId(receipt.getId());
            allocation.setSalesInvoiceId(invoice.getId());
            allocation.setAllocatedAmount(line.allocatedAmount());
            customerReceiptAllocationRepository.save(allocation);
            totalAllocated = totalAllocated.add(line.allocatedAmount());

            BigDecimal invoiceAllocated = customerReceiptAllocationRepository.findBySalesInvoiceIdOrderByIdAsc(invoice.getId()).stream()
                    .map(CustomerReceiptAllocation::getAllocatedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (invoiceAllocated.compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus(ErpDocumentStatuses.PAID);
            } else if (invoiceAllocated.compareTo(BigDecimal.ZERO) > 0) {
                invoice.setStatus(ErpDocumentStatuses.PARTIALLY_PAID);
            }
            salesInvoiceRepository.save(invoice);
            salesInvoicePaymentRequestService.synchronizeInvoiceRequests(invoice.getId());
        }

        if (totalAllocated.compareTo(receipt.getAmount()) > 0) {
            throw new BusinessException("Allocated amount cannot exceed receipt amount");
        }
        receipt.setStatus(totalAllocated.compareTo(receipt.getAmount()) == 0 ? ErpDocumentStatuses.ALLOCATED : ErpDocumentStatuses.POSTED);
        receipt = customerReceiptRepository.save(receipt);

        auditEventWriter.write(
                receipt.getOrganizationId(),
                receipt.getBranchId(),
                "CUSTOMER_RECEIPT_ALLOCATED",
                "customer_receipt",
                receipt.getId(),
                receipt.getReceiptNumber(),
                "ALLOCATE",
                null,
                receipt.getCustomerId(),
                null,
                "Customer receipt allocated",
                ErpJsonPayloads.of(
                        "receiptNumber", receipt.getReceiptNumber(),
                        "allocated", totalAllocated
                )
        );

        return receipt;
    }

    @Transactional(readOnly = true)
    public List<SalesInvoicePaymentRequest> listPaymentRequests(Long organizationId) {
        return salesInvoicePaymentRequestService.listPaymentRequests(organizationId);
    }

    @Transactional(readOnly = true)
    public List<SalesInvoicePaymentRequest> listInvoicePaymentRequests(Long salesInvoiceId) {
        return salesInvoicePaymentRequestService.listInvoicePaymentRequests(salesInvoiceId);
    }

    @Transactional(readOnly = true)
    public SalesInvoicePaymentRequest getPaymentRequest(Long paymentRequestId) {
        return salesInvoicePaymentRequestService.getPaymentRequest(paymentRequestId);
    }

    @Transactional(readOnly = true)
    public List<ErpSalesResponses.PaymentGatewayProviderResponse> listPaymentGatewayProviders() {
        return salesInvoicePaymentRequestService.listGatewayProviders();
    }

    public SalesInvoicePaymentRequest createPaymentRequest(Long organizationId,
                                                           Long branchId,
                                                           Long salesInvoiceId,
                                                           ErpSalesDtos.CreateSalesInvoicePaymentRequestRequest request) {
        return salesInvoicePaymentRequestService.createPaymentRequest(organizationId, branchId, salesInvoiceId, request);
    }

    public SalesInvoicePaymentRequest cancelPaymentRequest(Long paymentRequestId, ErpSalesDtos.CancelSalesInvoicePaymentRequest request) {
        SalesInvoicePaymentRequest paymentRequest = salesInvoicePaymentRequestService.getPaymentRequest(paymentRequestId);
        accessGuard.assertBranchAccess(paymentRequest.getOrganizationId(), paymentRequest.getBranchId());
        subscriptionAccessService.assertFeature(paymentRequest.getOrganizationId(), "payments");
        return salesInvoicePaymentRequestService.cancelPaymentRequest(paymentRequestId, request.reason());
    }

    public SalesInvoicePaymentRequest syncPaymentRequestProviderStatus(Long paymentRequestId) {
        SalesInvoicePaymentRequest paymentRequest = salesInvoicePaymentRequestService.getPaymentRequest(paymentRequestId);
        accessGuard.assertBranchAccess(paymentRequest.getOrganizationId(), paymentRequest.getBranchId());
        subscriptionAccessService.assertFeature(paymentRequest.getOrganizationId(), "payments");
        return salesInvoicePaymentRequestService.syncPaymentRequestProviderStatus(paymentRequestId);
    }

    @Transactional(readOnly = true)
    public ErpSalesResponses.SalesInvoicePaymentRequestSummaryResponse buildInvoicePaymentSummary(Long salesInvoiceId) {
        return salesInvoicePaymentRequestService.buildInvoicePaymentSummary(salesInvoiceId);
    }

    @Transactional(readOnly = true)
    public ErpSalesResponses.SalesInvoicePaymentRequestResponse toPaymentRequestResponse(SalesInvoicePaymentRequest paymentRequest) {
        return salesInvoicePaymentRequestService.toResponse(paymentRequest);
    }
    private ErpSalesResponses.SalesInvoiceResponse toInvoiceResponse(SalesInvoice invoice, List<SalesInvoiceLine> lines) {
        Map<Long, List<Long>> ownershipIdsByLine = new HashMap<>();
        for (SalesInvoiceLine line : lines) {
            ownershipIdsByLine.put(
                    line.getId(),
                    productOwnershipRepository.findBySalesInvoiceLineId(line.getId()).stream()
                            .map(ProductOwnership::getId)
                            .toList()
            );
        }
        return new ErpSalesResponses.SalesInvoiceResponse(
                invoice.getId(),
                invoice.getOrganizationId(),
                invoice.getBranchId(),
                invoice.getWarehouseId(),
                invoice.getCustomerId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                invoice.getSellerGstin(),
                invoice.getCustomerGstin(),
                invoice.getPlaceOfSupplyStateCode(),
                invoice.getSubtotal(),
                invoice.getDiscountAmount(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                allocatedAmount(invoice.getId()),
                outstandingAmount(invoice.getId(), invoice.getTotalAmount()),
                invoice.getStatus(),
                salesDispatchService.buildInvoiceDispatchSummary(invoice.getId()),
                salesInvoicePaymentRequestService.buildInvoicePaymentSummary(invoice.getId()),
                lines.stream()
                        .map(line -> new ErpSalesResponses.SalesInvoiceLineResponse(
                                line.getId(),
                                line.getProductId(),
                                line.getUomId(),
                                ownershipIdsByLine.getOrDefault(line.getId(), List.of()).size() == 1
                                        ? ownershipIdsByLine.get(line.getId()).getFirst()
                                        : null,
                                ownershipIdsByLine.getOrDefault(line.getId(), List.of()),
                                line.getHsnSnapshot(),
                                line.getQuantity(),
                                line.getBaseQuantity(),
                                line.getUnitPrice(),
                                line.getMrp(),
                                line.getDiscountAmount(),
                                line.getTaxableAmount(),
                                line.getTaxRate(),
                                line.getCgstRate(),
                                line.getCgstAmount(),
                                line.getSgstRate(),
                                line.getSgstAmount(),
                                line.getIgstRate(),
                                line.getIgstAmount(),
                                line.getCessRate(),
                                line.getCessAmount(),
                                line.getLineAmount()
                        ))
                        .toList(),
                invoiceAllocations(invoice.getId()),
                salesInvoicePaymentRequestService.toResponses(
                        salesInvoicePaymentRequestService.listInvoicePaymentRequests(invoice.getId())
                )
        );
    }

    private ErpSalesResponses.SalesQuoteResponse toQuoteResponse(SalesQuote quote, List<SalesQuoteLine> lines) {
        return new ErpSalesResponses.SalesQuoteResponse(
                quote.getId(),
                quote.getOrganizationId(),
                quote.getBranchId(),
                quote.getWarehouseId(),
                quote.getCustomerId(),
                quote.getQuoteType(),
                quote.getQuoteNumber(),
                quote.getQuoteDate(),
                quote.getValidUntil(),
                quote.getSellerGstin(),
                quote.getCustomerGstin(),
                quote.getPlaceOfSupplyStateCode(),
                quote.getSubtotal(),
                quote.getDiscountAmount(),
                quote.getTaxAmount(),
                quote.getTotalAmount(),
                quote.getConvertedSalesOrderId(),
                quote.getConvertedSalesInvoiceId(),
                quote.getStatus(),
                quote.getRemarks(),
                lines.stream().map(this::toSalesDocumentLineResponse).toList()
        );
    }

    private ErpSalesResponses.SalesOrderResponse toOrderResponse(SalesOrder order, List<SalesOrderLine> lines) {
        return new ErpSalesResponses.SalesOrderResponse(
                order.getId(),
                order.getOrganizationId(),
                order.getBranchId(),
                order.getWarehouseId(),
                order.getCustomerId(),
                order.getSourceQuoteId(),
                order.getOrderNumber(),
                order.getOrderDate(),
                order.getExpectedFulfillmentBy(),
                order.getSellerGstin(),
                order.getCustomerGstin(),
                order.getPlaceOfSupplyStateCode(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getConvertedSalesInvoiceId(),
                order.getStatus(),
                order.getRemarks(),
                lines.stream().map(this::toSalesDocumentLineResponse).toList()
        );
    }

    private ErpSalesResponses.SalesDocumentLineResponse toSalesDocumentLineResponse(SalesQuoteLine line) {
        return new ErpSalesResponses.SalesDocumentLineResponse(
                line.getId(), line.getProductId(), line.getUomId(), line.getHsnSnapshot(), line.getQuantity(), line.getBaseQuantity(),
                line.getUnitPrice(), line.getMrp(), line.getDiscountAmount(), line.getTaxableAmount(), line.getTaxRate(), line.getCgstRate(),
                line.getCgstAmount(), line.getSgstRate(), line.getSgstAmount(), line.getIgstRate(), line.getIgstAmount(),
                line.getCessRate(), line.getCessAmount(), line.getLineAmount(), line.getRemarks()
        );
    }

    private ErpSalesResponses.SalesDocumentLineResponse toSalesDocumentLineResponse(SalesOrderLine line) {
        return new ErpSalesResponses.SalesDocumentLineResponse(
                line.getId(), line.getProductId(), line.getUomId(), line.getHsnSnapshot(), line.getQuantity(), line.getBaseQuantity(),
                line.getUnitPrice(), line.getMrp(), line.getDiscountAmount(), line.getTaxableAmount(), line.getTaxRate(), line.getCgstRate(),
                line.getCgstAmount(), line.getSgstRate(), line.getSgstAmount(), line.getIgstRate(), line.getIgstAmount(),
                line.getCessRate(), line.getCessAmount(), line.getLineAmount(), line.getRemarks()
        );
    }

    private LocalDate resolveQuoteValidUntil(LocalDate quoteDate, LocalDate requestedValidUntil) {
        LocalDate validUntil = requestedValidUntil == null ? quoteDate.plusDays(DEFAULT_QUOTE_VALIDITY_DAYS) : requestedValidUntil;
        if (validUntil.isBefore(quoteDate)) {
            throw new BusinessException("Quote valid-until date cannot be before quote date");
        }
        return validUntil;
    }

    private LocalDate resolveOrderFulfillmentBy(LocalDate orderDate, LocalDate requestedFulfillmentBy) {
        LocalDate expectedFulfillmentBy = requestedFulfillmentBy == null
                ? orderDate.plusDays(DEFAULT_ORDER_FULFILLMENT_DAYS)
                : requestedFulfillmentBy;
        if (expectedFulfillmentBy.isBefore(orderDate)) {
            throw new BusinessException("Order fulfillment deadline cannot be before order date");
        }
        return expectedFulfillmentBy;
    }

    private String appendBundleRemark(String currentRemarks, String bundleName) {
        String bundleRemark = "Bundle: " + bundleName;
        if (currentRemarks == null || currentRemarks.isBlank()) {
            return bundleRemark;
        }
        if (currentRemarks.contains(bundleRemark)) {
            return currentRemarks;
        }
        return currentRemarks + " | " + bundleRemark;
    }

    private void ensureQuoteAvailableForConversion(SalesQuote quote, LocalDate referenceDate) {
        SalesQuote synchronizedQuote = synchronizeQuoteStatus(quote, referenceDate);
        if (ErpDocumentStatuses.EXPIRED.equals(synchronizedQuote.getStatus())) {
            throw new BusinessException("Sales quote has expired and cannot be converted");
        }
    }

    private void ensureOrderAvailableForInvoicing(SalesOrder order, LocalDate referenceDate) {
        SalesOrder synchronizedOrder = synchronizeOrderStatus(order, referenceDate);
        if (ErpDocumentStatuses.EXPIRED.equals(synchronizedOrder.getStatus())) {
            throw new BusinessException("Sales order has expired and cannot be converted to invoice");
        }
    }

    private SalesQuote synchronizeQuoteStatus(SalesQuote quote, LocalDate referenceDate) {
        if (!isQuoteExpirable(quote)) {
            return quote;
        }
        if (quote.getValidUntil() != null && quote.getValidUntil().isBefore(referenceDate)) {
            quote.setStatus(ErpDocumentStatuses.EXPIRED);
            return salesQuoteRepository.save(quote);
        }
        return quote;
    }

    private SalesOrder synchronizeOrderStatus(SalesOrder order, LocalDate referenceDate) {
        if (!isOrderExpirable(order)) {
            return order;
        }
        if (order.getExpectedFulfillmentBy() != null && order.getExpectedFulfillmentBy().isBefore(referenceDate)) {
            order.setStatus(ErpDocumentStatuses.EXPIRED);
            return salesOrderRepository.save(order);
        }
        return order;
    }

    private boolean isQuoteExpirable(SalesQuote quote) {
        return !ErpDocumentStatuses.CANCELLED.equals(quote.getStatus())
                && !ErpDocumentStatuses.EXPIRED.equals(quote.getStatus())
                && !"ORDERED".equals(quote.getStatus())
                && !"INVOICED".equals(quote.getStatus());
    }

    private boolean isOrderExpirable(SalesOrder order) {
        return !ErpDocumentStatuses.CANCELLED.equals(order.getStatus())
                && !ErpDocumentStatuses.EXPIRED.equals(order.getStatus())
                && !"INVOICED".equals(order.getStatus());
    }

    private Totals saveQuoteLines(SalesQuote quote, Customer customer, List<ErpSalesDtos.CreateSalesDocumentLineRequest> requestLines) {
        List<ErpSalesDtos.CreateSalesDocumentLineRequest> expandedLines = expandDocumentLines(
                quote.getOrganizationId(),
                customer.getId(),
                quote.getQuoteDate(),
                requestLines
        );
        return saveDocumentLines(
                quote.getOrganizationId(),
                quote.getBranchId(),
                quote.getWarehouseId(),
                quote.getQuoteDate(),
                quote.getPlaceOfSupplyStateCode(),
                customer,
                expandedLines,
                (reqLine, pricing, master, taxContext) -> {
                    SalesQuoteLine line = new SalesQuoteLine();
                    line.setSalesQuoteId(quote.getId());
                    line.setProductId(reqLine.productId());
                    line.setUomId(reqLine.uomId());
                    line.setHsnSnapshot(master.getHsnCode());
                    applyDocumentPricing(line, reqLine, pricing, taxContext);
                    line.setRemarks(reqLine.remarks());
                    salesQuoteLineRepository.save(line);
                }
        );
    }

    private Totals saveOrderLines(SalesOrder order,
                                  Customer customer,
                                  List<ErpSalesDtos.CreateSalesDocumentLineRequest> requestLines,
                                  List<SalesQuoteLine> sourceQuoteLines) {
        List<ErpSalesDtos.CreateSalesDocumentLineRequest> expandedLines = expandDocumentLines(
                order.getOrganizationId(),
                customer.getId(),
                order.getOrderDate(),
                requestLines
        );
        return saveDocumentLines(
                order.getOrganizationId(),
                order.getBranchId(),
                order.getWarehouseId(),
                order.getOrderDate(),
                order.getPlaceOfSupplyStateCode(),
                customer,
                expandedLines,
                (reqLine, pricing, master, taxContext) -> {
                    SalesOrderLine line = new SalesOrderLine();
                    line.setSalesOrderId(order.getId());
                    if (sourceQuoteLines != null) {
                        SalesQuoteLine match = sourceQuoteLines.stream()
                                .filter(source -> source.getProductId().equals(reqLine.productId()) && source.getUomId().equals(reqLine.uomId()))
                                .findFirst()
                                .orElse(null);
                        line.setSourceQuoteLineId(match == null ? null : match.getId());
                    }
                    line.setProductId(reqLine.productId());
                    line.setUomId(reqLine.uomId());
                    line.setHsnSnapshot(master.getHsnCode());
                    applyDocumentPricing(line, reqLine, pricing, taxContext);
                    line.setRemarks(reqLine.remarks());
                    salesOrderLineRepository.save(line);
                }
        );
    }

    private List<ErpSalesDtos.CreateSalesDocumentLineRequest> expandDocumentLines(Long organizationId,
                                                                                  Long customerId,
                                                                                  LocalDate documentDate,
                                                                                  List<ErpSalesDtos.CreateSalesDocumentLineRequest> requestLines) {
        List<ErpSalesDtos.CreateSalesDocumentLineRequest> expanded = new ArrayList<>();
        for (ErpSalesDtos.CreateSalesDocumentLineRequest line : requestLines) {
            expanded.addAll(expandBundleLine(organizationId, customerId, documentDate, line));
        }
        return expanded;
    }

    private List<ErpSalesDtos.CreateSalesInvoiceLineRequest> expandInvoiceLines(Long organizationId,
                                                                                 Long customerId,
                                                                                 LocalDate documentDate,
                                                                                 List<ErpSalesDtos.CreateSalesInvoiceLineRequest> requestLines) {
        List<ErpSalesDtos.CreateSalesInvoiceLineRequest> expanded = new ArrayList<>();
        for (ErpSalesDtos.CreateSalesInvoiceLineRequest line : requestLines) {
            StoreProduct product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.productId()));
            if (!Boolean.TRUE.equals(product.getIsBundle())) {
                expanded.add(line);
                continue;
            }
            List<ErpSalesDtos.CreateSalesDocumentLineRequest> exploded = expandBundleLine(
                    organizationId,
                    customerId,
                    documentDate,
                    new ErpSalesDtos.CreateSalesDocumentLineRequest(
                            line.productId(),
                            line.uomId(),
                            line.quantity(),
                            line.baseQuantity(),
                            line.discountAmount(),
                            "Bundle: " + product.getName()
                    )
            );
            for (ErpSalesDtos.CreateSalesDocumentLineRequest explodedLine : exploded) {
                expanded.add(new ErpSalesDtos.CreateSalesInvoiceLineRequest(
                        explodedLine.productId(),
                        explodedLine.uomId(),
                        explodedLine.quantity(),
                        explodedLine.baseQuantity(),
                        explodedLine.discountAmount(),
                        null,
                        null,
                        line.warrantyMonths()
                ));
            }
        }
        return expanded;
    }

    private List<ErpSalesDtos.CreateSalesDocumentLineRequest> expandBundleLine(Long organizationId,
                                                                               Long customerId,
                                                                               LocalDate documentDate,
                                                                               ErpSalesDtos.CreateSalesDocumentLineRequest line) {
        StoreProduct product = productRepository.findById(line.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.productId()));
        if (!Boolean.TRUE.equals(product.getIsBundle())) {
            return List.of(line);
        }
        List<StoreProductBundleComponent> components = storeProductBundleComponentRepository
                .findByOrganizationIdAndStoreProductIdOrderBySortOrderAscIdAsc(organizationId, product.getId());
        if (components.isEmpty()) {
            throw new BusinessException("Bundle product has no components configured: " + product.getSku());
        }

        List<BigDecimal> baseValues = new ArrayList<>();
        BigDecimal totalBaseValue = BigDecimal.ZERO;
        for (StoreProductBundleComponent component : components) {
            BigDecimal componentBaseQuantity = component.getComponentBaseQuantity().multiply(line.baseQuantity());
            BigDecimal componentUnitPrice = storeProductPricingService.resolveUnitPrice(
                    organizationId,
                    component.getComponentStoreProductId(),
                    customerId,
                    componentBaseQuantity,
                    documentDate
            );
            StoreProduct componentProduct = productRepository.findById(component.getComponentStoreProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bundle component product not found: " + component.getComponentStoreProductId()));
            BigDecimal componentQuantity = component.getComponentQuantity().multiply(line.quantity());
            BigDecimal baseValue = componentUnitPrice.multiply(componentQuantity);
            if (baseValue.compareTo(BigDecimal.ZERO) == 0 && componentProduct.getDefaultSalePrice() != null) {
                baseValue = componentProduct.getDefaultSalePrice().multiply(componentQuantity);
            }
            baseValues.add(baseValue);
            totalBaseValue = totalBaseValue.add(baseValue);
        }

        List<ErpSalesDtos.CreateSalesDocumentLineRequest> expanded = new ArrayList<>();
        BigDecimal remainingDiscount = line.discountAmount() == null ? BigDecimal.ZERO : line.discountAmount();
        for (int index = 0; index < components.size(); index++) {
            StoreProductBundleComponent component = components.get(index);
            StoreProduct componentProduct = productRepository.findById(component.getComponentStoreProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bundle component product not found: " + component.getComponentStoreProductId()));
            BigDecimal allocatedDiscount;
            if (index == components.size() - 1) {
                allocatedDiscount = remainingDiscount;
            } else if (remainingDiscount.compareTo(BigDecimal.ZERO) == 0) {
                allocatedDiscount = BigDecimal.ZERO;
            } else if (totalBaseValue.compareTo(BigDecimal.ZERO) > 0) {
                allocatedDiscount = line.discountAmount()
                        .multiply(baseValues.get(index))
                        .divide(totalBaseValue, 2, RoundingMode.HALF_UP);
                remainingDiscount = remainingDiscount.subtract(allocatedDiscount);
            } else {
                BigDecimal divisor = BigDecimal.valueOf(components.size() - index);
                allocatedDiscount = remainingDiscount.divide(divisor, 2, RoundingMode.HALF_UP);
                remainingDiscount = remainingDiscount.subtract(allocatedDiscount);
            }
            expanded.add(new ErpSalesDtos.CreateSalesDocumentLineRequest(
                    componentProduct.getId(),
                    componentProduct.getBaseUomId(),
                    component.getComponentQuantity().multiply(line.quantity()),
                    component.getComponentBaseQuantity().multiply(line.baseQuantity()),
                    allocatedDiscount,
                    appendBundleRemark(line.remarks(), product.getName())
            ));
        }
        return expanded;
    }

    private Totals saveDocumentLines(Long organizationId,
                                     Long branchId,
                                     Long warehouseId,
                                     LocalDate documentDate,
                                     String placeOfSupplyStateCode,
                                     Customer customer,
                                     List<ErpSalesDtos.CreateSalesDocumentLineRequest> requestLines,
                                     DocumentLineSaver saver) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        Long sellerTaxRegistrationId = null;
        String sellerGstin = null;
        String resolvedPos = placeOfSupplyStateCode;

        for (ErpSalesDtos.CreateSalesDocumentLineRequest reqLine : requestLines) {
            StoreProduct product = productRepository.findById(reqLine.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + reqLine.productId()));
            Product productMaster = productGovernanceGuard.requireTransactionAllowed(product, "sales transactions");
            uomRepository.findById(reqLine.uomId())
                    .orElseThrow(() -> new ResourceNotFoundException("UOM not found: " + reqLine.uomId()));

            BigDecimal unitPrice = storeProductPricingService.resolveUnitPrice(
                    organizationId,
                    reqLine.productId(),
                    customer.getId(),
                    reqLine.baseQuantity(),
                    documentDate
            );
            BigDecimal lineDiscount = reqLine.discountAmount() == null ? BigDecimal.ZERO : reqLine.discountAmount();
            BigDecimal lineBase = unitPrice.multiply(reqLine.quantity());
            BigDecimal lineTaxable = lineBase.subtract(lineDiscount);
            GstTaxService.TaxContext taxContext = gstTaxService.resolveSalesTax(
                    organizationId,
                    branchId,
                    documentDate,
                    productMaster.getHsnCode(),
                    product.getTaxGroupId(),
                    customer.getGstin(),
                    placeOfSupplyStateCode,
                    lineTaxable
            );

            if (sellerTaxRegistrationId == null) {
                sellerTaxRegistrationId = taxContext.sellerTaxRegistrationId();
                sellerGstin = taxContext.sellerGstin();
                resolvedPos = taxContext.placeOfSupplyStateCode();
            }

            validateMrpCeiling(product, product.getDefaultMrp(), reqLine.quantity(), taxContext.lineTotal());
            saver.save(reqLine, new PricingSnapshot(unitPrice, product.getDefaultMrp(), lineDiscount), productMaster, taxContext);
            subtotal = subtotal.add(lineBase);
            discount = discount.add(lineDiscount);
            taxAmount = taxAmount.add(taxContext.totalTaxAmount());
            total = total.add(taxContext.lineTotal());
        }

        return new Totals(subtotal, discount, taxAmount, total, sellerTaxRegistrationId, sellerGstin, resolvedPos);
    }

    private void applyDocumentPricing(SalesQuoteLine line,
                                      ErpSalesDtos.CreateSalesDocumentLineRequest reqLine,
                                      PricingSnapshot pricing,
                                      GstTaxService.TaxContext taxContext) {
        line.setQuantity(reqLine.quantity());
        line.setBaseQuantity(reqLine.baseQuantity());
        line.setUnitPrice(pricing.unitPrice());
        line.setMrp(pricing.mrp());
        line.setDiscountAmount(pricing.discountAmount());
        line.setTaxableAmount(taxContext.taxableAmount());
        line.setTaxRate(taxContext.effectiveTaxRate());
        line.setCgstRate(taxContext.cgstRate());
        line.setCgstAmount(taxContext.cgstAmount());
        line.setSgstRate(taxContext.sgstRate());
        line.setSgstAmount(taxContext.sgstAmount());
        line.setIgstRate(taxContext.igstRate());
        line.setIgstAmount(taxContext.igstAmount());
        line.setCessRate(taxContext.cessRate());
        line.setCessAmount(taxContext.cessAmount());
        line.setLineAmount(taxContext.lineTotal());
    }

    private void applyDocumentPricing(SalesOrderLine line,
                                      ErpSalesDtos.CreateSalesDocumentLineRequest reqLine,
                                      PricingSnapshot pricing,
                                      GstTaxService.TaxContext taxContext) {
        line.setQuantity(reqLine.quantity());
        line.setBaseQuantity(reqLine.baseQuantity());
        line.setUnitPrice(pricing.unitPrice());
        line.setMrp(pricing.mrp());
        line.setDiscountAmount(pricing.discountAmount());
        line.setTaxableAmount(taxContext.taxableAmount());
        line.setTaxRate(taxContext.effectiveTaxRate());
        line.setCgstRate(taxContext.cgstRate());
        line.setCgstAmount(taxContext.cgstAmount());
        line.setSgstRate(taxContext.sgstRate());
        line.setSgstAmount(taxContext.sgstAmount());
        line.setIgstRate(taxContext.igstRate());
        line.setIgstAmount(taxContext.igstAmount());
        line.setCessRate(taxContext.cessRate());
        line.setCessAmount(taxContext.cessAmount());
        line.setLineAmount(taxContext.lineTotal());
    }

    private List<ErpSalesDtos.CreateSalesDocumentLineRequest> toDocumentLineRequests(List<SalesQuoteLine> quoteLines) {
        return quoteLines.stream()
                .map(line -> new ErpSalesDtos.CreateSalesDocumentLineRequest(
                        line.getProductId(),
                        line.getUomId(),
                        line.getQuantity(),
                        line.getBaseQuantity(),
                        line.getDiscountAmount(),
                        line.getRemarks()
                ))
                .toList();
    }

    private List<ErpSalesDtos.CreateSalesInvoiceLineRequest> toInvoiceLineRequests(List<SalesQuoteLine> quoteLines,
                                                                                   String sourceNumber,
                                                                                   Map<Long, ErpSalesDtos.ConvertTrackedSalesLineRequest> trackedSelectionsByProduct) {
        return quoteLines.stream()
                .map(line -> new ErpSalesDtos.CreateSalesInvoiceLineRequest(
                        line.getProductId(),
                        line.getUomId(),
                        line.getQuantity(),
                        line.getBaseQuantity(),
                        line.getDiscountAmount(),
                        trackedSelectionsByProduct.get(line.getProductId()) == null ? null : trackedSelectionsByProduct.get(line.getProductId()).serialNumberIds(),
                        trackedSelectionsByProduct.get(line.getProductId()) == null ? null : trackedSelectionsByProduct.get(line.getProductId()).batchSelections(),
                        null
                ))
                .toList();
    }

    private List<ErpSalesDtos.CreateSalesInvoiceLineRequest> toInvoiceLineRequestsFromOrder(List<SalesOrderLine> orderLines,
                                                                                            String sourceNumber,
                                                                                            Map<Long, ErpSalesDtos.ConvertTrackedSalesLineRequest> trackedSelectionsByProduct) {
        return orderLines.stream()
                .map(line -> new ErpSalesDtos.CreateSalesInvoiceLineRequest(
                        line.getProductId(),
                        line.getUomId(),
                        line.getQuantity(),
                        line.getBaseQuantity(),
                        line.getDiscountAmount(),
                        trackedSelectionsByProduct.get(line.getProductId()) == null ? null : trackedSelectionsByProduct.get(line.getProductId()).serialNumberIds(),
                        trackedSelectionsByProduct.get(line.getProductId()) == null ? null : trackedSelectionsByProduct.get(line.getProductId()).batchSelections(),
                        null
                ))
                .toList();
    }

    private Map<Long, ErpSalesDtos.ConvertTrackedSalesLineRequest> trackedSelectionsByProduct(List<ErpSalesDtos.ConvertTrackedSalesLineRequest> trackedLines) {
        Map<Long, ErpSalesDtos.ConvertTrackedSalesLineRequest> result = new HashMap<>();
        if (trackedLines == null) {
            return result;
        }
        for (ErpSalesDtos.ConvertTrackedSalesLineRequest trackedLine : trackedLines) {
            if (result.put(trackedLine.productId(), trackedLine) != null) {
                throw new BusinessException("Duplicate tracked selection for product " + trackedLine.productId());
            }
        }
        return result;
    }

    private void validateQuoteType(String quoteType) {
        String normalized = quoteType == null ? "" : quoteType.trim().toUpperCase();
        if (!"ESTIMATE".equals(normalized) && !"QUOTATION".equals(normalized)) {
            throw new BusinessException("Quote type must be ESTIMATE or QUOTATION");
        }
    }

    private String quoteNumber(String quoteType) {
        String prefix = "ESTIMATE".equals(quoteType == null ? "" : quoteType.trim().toUpperCase()) ? "EST" : "QTN";
        return prefix + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
    }

    private BigDecimal allocatedAmount(Long salesInvoiceId) {
        return customerReceiptAllocationRepository.findBySalesInvoiceIdOrderByIdAsc(salesInvoiceId).stream()
                .map(CustomerReceiptAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal outstandingAmount(Long salesInvoiceId, BigDecimal totalAmount) {
        return totalAmount.subtract(allocatedAmount(salesInvoiceId)).max(BigDecimal.ZERO);
    }

    private List<ErpSalesResponses.SalesInvoiceAllocationResponse> invoiceAllocations(Long salesInvoiceId) {
        return customerReceiptAllocationRepository.findBySalesInvoiceIdOrderByIdAsc(salesInvoiceId).stream()
                .map(allocation -> {
                    CustomerReceipt receipt = customerReceiptRepository.findById(allocation.getCustomerReceiptId()).orElse(null);
                    if (receipt == null) {
                        return new ErpSalesResponses.SalesInvoiceAllocationResponse(
                                allocation.getCustomerReceiptId(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                allocation.getAllocatedAmount(),
                                null
                        );
                    }
                    return new ErpSalesResponses.SalesInvoiceAllocationResponse(
                            receipt.getId(),
                            receipt.getReceiptNumber(),
                            receipt.getReceiptDate(),
                            receipt.getPaymentMethod(),
                            receipt.getReferenceNumber(),
                            receipt.getAmount(),
                            allocation.getAllocatedAmount(),
                            receipt.getStatus()
                    );
                })
                .toList();
    }

    private String appendCancellationReason(String currentRemarks, String reason) {
        if (currentRemarks == null || currentRemarks.isBlank()) {
            return "Cancelled: " + reason;
        }
        return currentRemarks.trim() + " | Cancelled: " + reason;
    }

    private record Totals(
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal taxAmount,
            BigDecimal total,
            Long sellerTaxRegistrationId,
            String sellerGstin,
            String placeOfSupplyStateCode
    ) {}

    private record PricingSnapshot(BigDecimal unitPrice, BigDecimal mrp, BigDecimal discountAmount) {}

    private record BatchAllocation(Long batchId, BigDecimal quantity, BigDecimal baseQuantity) {}

    private record ResolvedInventoryPricing(BigDecimal unitPrice, BigDecimal mrp, List<BatchAllocation> batchAllocations) {}

    @FunctionalInterface
    private interface DocumentLineSaver {
        void save(ErpSalesDtos.CreateSalesDocumentLineRequest reqLine,
                  PricingSnapshot pricing,
                  Product productMaster,
                  GstTaxService.TaxContext taxContext);
    }
}
