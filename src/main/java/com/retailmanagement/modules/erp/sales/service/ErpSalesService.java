package com.retailmanagement.modules.erp.sales.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.approval.dto.ErpApprovalDtos;
import com.retailmanagement.modules.erp.approval.service.ErpApprovalService;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.entity.Product;
import com.retailmanagement.modules.erp.catalog.repository.ProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.UomRepository;
import com.retailmanagement.modules.erp.catalog.service.StoreProductPricingService;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.finance.service.ErpAccountingPostingService;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
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
    private final ProductRepository productMasterRepository;
    private final CustomerRepository customerRepository;
    private final StoreCustomerTermsRepository storeCustomerTermsRepository;
    private final UomRepository uomRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryReservationService inventoryReservationService;
    private final AuditEventWriter auditEventWriter;
    private final ErpAccessGuard accessGuard;
    private final SubscriptionAccessService subscriptionAccessService;
    private final GstTaxService gstTaxService;
    private final StoreProductPricingService storeProductPricingService;
    private final ErpAccountingPostingService accountingPostingService;
    private final SalesInvoicePostingService salesInvoicePostingService;
    private final ErpApprovalService erpApprovalService;

    @Transactional(readOnly = true)
    public List<SalesInvoice> listInvoices(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        return salesInvoiceRepository.findTop100ByOrganizationIdOrderByInvoiceDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public List<SalesQuote> listQuotes(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        return salesQuoteRepository.findTop100ByOrganizationIdOrderByQuoteDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public ErpSalesResponses.SalesQuoteResponse getQuote(Long id) {
        SalesQuote quote = salesQuoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales quote not found: " + id));
        accessGuard.assertOrganizationAccess(quote.getOrganizationId());
        accessGuard.assertBranchAccess(quote.getOrganizationId(), quote.getBranchId());
        subscriptionAccessService.assertFeature(quote.getOrganizationId(), "sales");
        return toQuoteResponse(quote, salesQuoteLineRepository.findBySalesQuoteIdOrderByIdAsc(id));
    }

    @Transactional(readOnly = true)
    public List<SalesOrder> listOrders(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        return salesOrderRepository.findTop100ByOrganizationIdOrderByOrderDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public ErpSalesResponses.SalesOrderResponse getOrder(Long id) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found: " + id));
        accessGuard.assertOrganizationAccess(order.getOrganizationId());
        accessGuard.assertBranchAccess(order.getOrganizationId(), order.getBranchId());
        subscriptionAccessService.assertFeature(order.getOrganizationId(), "sales");
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
        var principal = enforceUserContext ? ErpSecurityUtils.requirePrincipal() : null;
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
        List<String> priceOverrides = new ArrayList<>();

        for (ErpSalesDtos.CreateSalesInvoiceLineRequest reqLine : request.lines()) {
            StoreProduct product = productRepository.findById(reqLine.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + reqLine.productId()));
            Product productMaster = productMasterRepository.findById(product.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product master not found: " + product.getProductId()));
            uomRepository.findById(reqLine.uomId())
                    .orElseThrow(() -> new ResourceNotFoundException("UOM not found: " + reqLine.uomId()));
            validateInvoiceTracking(product, reqLine);

            BigDecimal resolvedUnitPrice = reqLine.unitPrice() != null
                    ? reqLine.unitPrice()
                    : storeProductPricingService.resolveUnitPrice(
                            organizationId,
                            reqLine.productId(),
                            request.customerId(),
                            reqLine.baseQuantity(),
                            request.invoiceDate() == null ? LocalDate.now() : request.invoiceDate()
                    );
            if (enforceUserContext && reqLine.unitPrice() != null) {
                validatePriceOverride(principal, product, resolvedUnitPrice, reqLine.unitPrice(), reqLine.priceOverrideReason());
                if (reqLine.unitPrice().compareTo(resolvedUnitPrice) != 0) {
                    priceOverrides.add(product.getSku() + ":" + resolvedUnitPrice + "->" + reqLine.unitPrice() + ":" + reqLine.priceOverrideReason().trim());
                }
            }
            BigDecimal lineDiscount = reqLine.discountAmount() == null ? BigDecimal.ZERO : reqLine.discountAmount();
            BigDecimal lineBase = resolvedUnitPrice.multiply(reqLine.quantity());
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

            if (reqLine.batchSelections() != null && !reqLine.batchSelections().isEmpty()) {
                BigDecimal batchBase = BigDecimal.ZERO;
                for (ErpSalesDtos.BatchSelection batchSelection : reqLine.batchSelections()) {
                    InventoryBatch batch = inventoryBatchRepository.findById(batchSelection.batchId())
                            .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchSelection.batchId()));
                    if (!organizationId.equals(batch.getOrganizationId())) {
                        throw new BusinessException("Batch " + batchSelection.batchId() + " does not belong to organization " + organizationId);
                    }
                    if (!reqLine.productId().equals(batch.getProductId())) {
                        throw new BusinessException("Batch " + batchSelection.batchId() + " does not belong to product " + reqLine.productId());
                    }
                    SalesLineBatch link = new SalesLineBatch();
                    link.setSalesInvoiceLineId(line.getId());
                    link.setBatchId(batchSelection.batchId());
                    link.setQuantity(batchSelection.quantity());
                    link.setBaseQuantity(batchSelection.baseQuantity());
                    salesLineBatchRepository.save(link);
                    batchBase = batchBase.add(batchSelection.baseQuantity());
                }
                if (batchBase.compareTo(reqLine.baseQuantity()) != 0) {
                    throw new BusinessException("Batch base quantity mismatch for product " + reqLine.productId());
                }
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

        if (enforceUserContext && !priceOverrides.isEmpty()) {
            auditEventWriter.write(
                    organizationId,
                    branchId,
                    "SALES_PRICE_OVERRIDDEN",
                    "sales_invoice",
                    invoice.getId(),
                    invoice.getInvoiceNumber(),
                    "PRICE_OVERRIDE",
                    invoice.getWarehouseId(),
                    invoice.getCustomerId(),
                    null,
                    "Sales invoice contains manual price override",
                    ErpJsonPayloads.of(
                            "invoiceId", invoice.getId(),
                            "invoiceNumber", invoice.getInvoiceNumber(),
                            "overrideCount", priceOverrides.size(),
                            "overrides", priceOverrides
                    )
            );
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
        quote.setQuoteDate(request.quoteDate() == null ? LocalDate.now() : request.quoteDate());
        quote.setValidUntil(request.validUntil());
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
        order.setOrderDate(request.orderDate() == null ? LocalDate.now() : request.orderDate());
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
        order.setOrderDate(request.targetDate() == null ? LocalDate.now() : request.targetDate());
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

        if (hasSerials || hasBatches) {
            throw new BusinessException("Standard product does not accept serial or batch details: " + product.getSku());
        }
    }

    private void validatePriceOverride(com.retailmanagement.modules.auth.security.UserPrincipal principal,
                                       StoreProduct product,
                                       BigDecimal resolvedUnitPrice,
                                       BigDecimal requestedUnitPrice,
                                       String overrideReason) {
        if (requestedUnitPrice.compareTo(resolvedUnitPrice) == 0) {
            return;
        }
        if (overrideReason == null || overrideReason.isBlank()) {
            throw new BusinessException("Price override reason is required for product " + product.getSku());
        }
        if (principal.hasRole("OWNER") || principal.hasRole("ADMIN") || principal.hasRole("STORE_MANAGER")) {
            return;
        }
        throw new BusinessException("You are not allowed to override the resolved selling price for product " + product.getSku());
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
                invoiceAllocations(invoice.getId())
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
                line.getUnitPrice(), line.getDiscountAmount(), line.getTaxableAmount(), line.getTaxRate(), line.getCgstRate(),
                line.getCgstAmount(), line.getSgstRate(), line.getSgstAmount(), line.getIgstRate(), line.getIgstAmount(),
                line.getCessRate(), line.getCessAmount(), line.getLineAmount(), line.getRemarks()
        );
    }

    private ErpSalesResponses.SalesDocumentLineResponse toSalesDocumentLineResponse(SalesOrderLine line) {
        return new ErpSalesResponses.SalesDocumentLineResponse(
                line.getId(), line.getProductId(), line.getUomId(), line.getHsnSnapshot(), line.getQuantity(), line.getBaseQuantity(),
                line.getUnitPrice(), line.getDiscountAmount(), line.getTaxableAmount(), line.getTaxRate(), line.getCgstRate(),
                line.getCgstAmount(), line.getSgstRate(), line.getSgstAmount(), line.getIgstRate(), line.getIgstAmount(),
                line.getCessRate(), line.getCessAmount(), line.getLineAmount(), line.getRemarks()
        );
    }

    private Totals saveQuoteLines(SalesQuote quote, Customer customer, List<ErpSalesDtos.CreateSalesDocumentLineRequest> requestLines) {
        return saveDocumentLines(
                quote.getOrganizationId(),
                quote.getBranchId(),
                quote.getWarehouseId(),
                quote.getQuoteDate(),
                quote.getPlaceOfSupplyStateCode(),
                customer,
                requestLines,
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
        return saveDocumentLines(
                order.getOrganizationId(),
                order.getBranchId(),
                order.getWarehouseId(),
                order.getOrderDate(),
                order.getPlaceOfSupplyStateCode(),
                customer,
                requestLines,
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
            Product productMaster = productMasterRepository.findById(product.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product master not found: " + product.getProductId()));
            uomRepository.findById(reqLine.uomId())
                    .orElseThrow(() -> new ResourceNotFoundException("UOM not found: " + reqLine.uomId()));

            BigDecimal unitPrice = reqLine.unitPrice() != null
                    ? reqLine.unitPrice()
                    : storeProductPricingService.resolveUnitPrice(
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

            saver.save(reqLine, new PricingSnapshot(unitPrice, lineDiscount), productMaster, taxContext);
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
                        line.getUnitPrice(),
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
                        line.getUnitPrice(),
                        "Converted from " + sourceNumber,
                        line.getTaxRate(),
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
                        line.getUnitPrice(),
                        "Converted from " + sourceNumber,
                        line.getTaxRate(),
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

    private record PricingSnapshot(BigDecimal unitPrice, BigDecimal discountAmount) {}

    @FunctionalInterface
    private interface DocumentLineSaver {
        void save(ErpSalesDtos.CreateSalesDocumentLineRequest reqLine,
                  PricingSnapshot pricing,
                  Product productMaster,
                  GstTaxService.TaxContext taxContext);
    }
}
