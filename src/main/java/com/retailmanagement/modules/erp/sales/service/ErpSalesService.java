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
import java.util.List;
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
    private final CustomerReceiptRepository customerReceiptRepository;
    private final CustomerReceiptAllocationRepository customerReceiptAllocationRepository;
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
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        var principal = ErpSecurityUtils.requirePrincipal();
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
            if (reqLine.unitPrice() != null) {
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
            line.setWarrantyMonths(reqLine.warrantyMonths());
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

        if (!priceOverrides.isEmpty()) {
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

    @Transactional(readOnly = true)
    public List<CustomerReceipt> listReceipts(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "payments");
        return customerReceiptRepository.findTop100ByOrganizationIdOrderByReceiptDateDescIdDesc(organizationId);
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
                        .toList()
        );
    }

    private BigDecimal allocatedAmount(Long salesInvoiceId) {
        return customerReceiptAllocationRepository.findBySalesInvoiceIdOrderByIdAsc(salesInvoiceId).stream()
                .map(CustomerReceiptAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal outstandingAmount(Long salesInvoiceId, BigDecimal totalAmount) {
        return totalAmount.subtract(allocatedAmount(salesInvoiceId)).max(BigDecimal.ZERO);
    }
}
