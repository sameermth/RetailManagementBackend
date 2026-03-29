package com.retailmanagement.modules.erp.purchase.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.approval.dto.ErpApprovalDtos;
import com.retailmanagement.modules.erp.approval.service.ErpApprovalService;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.entity.Product;
import com.retailmanagement.modules.erp.catalog.repository.ProductRepository;
import com.retailmanagement.modules.erp.finance.service.ErpAccountingPostingService;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.constants.ErpInventoryMovementTypes;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.UomRepository;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBatchRepository;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.inventory.service.InventoryPostingService;
import com.retailmanagement.modules.erp.purchase.dto.ErpPurchaseDtos;
import com.retailmanagement.modules.erp.purchase.dto.ErpPurchaseResponses;
import com.retailmanagement.modules.erp.purchase.entity.*;
import com.retailmanagement.modules.erp.purchase.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import com.retailmanagement.modules.erp.party.entity.SupplierProduct;
import com.retailmanagement.modules.erp.party.repository.SupplierProductRepository;
import com.retailmanagement.modules.erp.party.repository.StoreSupplierTermsRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import com.retailmanagement.modules.erp.tax.service.GstTaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ErpPurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final PurchaseReceiptLineRepository purchaseReceiptLineRepository;
    private final PurchaseReceiptLineSerialRepository purchaseReceiptLineSerialRepository;
    private final PurchaseReceiptLineBatchRepository purchaseReceiptLineBatchRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierPaymentAllocationRepository supplierPaymentAllocationRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final StoreSupplierTermsRepository storeSupplierTermsRepository;
    private final StoreProductRepository productRepository;
    private final ProductRepository productMasterRepository;
    private final UomRepository uomRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final InventoryPostingService inventoryPostingService;
    private final AuditEventWriter auditEventWriter;
    private final ErpAccessGuard accessGuard;
    private final SubscriptionAccessService subscriptionAccessService;
    private final GstTaxService gstTaxService;
    private final ErpAccountingPostingService accountingPostingService;
    private final ErpApprovalService approvalService;

    @Transactional(readOnly = true)
    public List<PurchaseOrder> listPurchaseOrders(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "purchases");
        return purchaseOrderRepository.findTop100ByOrganizationIdOrderByPoDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public ErpPurchaseResponses.PurchaseOrderResponse getPurchaseOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));
        accessGuard.assertOrganizationAccess(order.getOrganizationId());
        accessGuard.assertBranchAccess(order.getOrganizationId(), order.getBranchId());
        subscriptionAccessService.assertFeature(order.getOrganizationId(), "purchases");
        List<PurchaseOrderLine> lines = purchaseOrderLineRepository.findByPurchaseOrderIdOrderByIdAsc(id);
        return toPurchaseOrderResponse(order, lines);
    }

    public ErpPurchaseResponses.PurchaseOrderResponse createPurchaseOrder(Long organizationId, Long branchId, ErpPurchaseDtos.CreatePurchaseOrderRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "purchases");
        var supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));
        ensureSupplierBelongsToOrganization(organizationId, supplier.getId());

        String poNumber = "PO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();

        PurchaseOrder order = new PurchaseOrder();
        order.setOrganizationId(organizationId);
        order.setBranchId(branchId);
        order.setSupplierId(request.supplierId());
        order.setPoNumber(poNumber);
        order.setPoDate(request.poDate() == null ? LocalDate.now() : request.poDate());
        order.setSupplierGstin(supplier.getGstin());
        order.setPlaceOfSupplyStateCode(request.placeOfSupplyStateCode());
        order.setStatus(ErpDocumentStatuses.SUBMITTED);
        order.setRemarks(request.remarks());
        order.setSubmittedAt(LocalDateTime.now());
        order.setSubmittedBy(ErpSecurityUtils.currentUserId().orElse(1L));
        order = purchaseOrderRepository.save(order);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (ErpPurchaseDtos.CreatePurchaseOrderLineRequest reqLine : request.lines()) {
            StoreProduct product = productRepository.findById(reqLine.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + reqLine.productId()));
            ensureStoreProductBelongsToOrganization(organizationId, product);
            Product productMaster = productMasterRepository.findById(product.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product master not found: " + product.getProductId()));
            SupplierProduct supplierProduct = resolveSupplierProduct(organizationId, supplier.getId(), product.getProductId(), reqLine.supplierProductId());
            uomRepository.findById(reqLine.uomId())
                    .orElseThrow(() -> new ResourceNotFoundException("UOM not found: " + reqLine.uomId()));

            BigDecimal lineBase = reqLine.unitPrice().multiply(reqLine.quantity());
            GstTaxService.TaxContext taxContext = gstTaxService.resolvePurchaseTax(
                    organizationId,
                    branchId,
                    order.getPoDate(),
                    product.getTaxGroupId(),
                    supplier.getGstin(),
                    request.placeOfSupplyStateCode(),
                    lineBase
            );
            BigDecimal lineTax = taxContext.totalTaxAmount();
            BigDecimal lineTotal = taxContext.lineTotal();

            if (order.getSellerTaxRegistrationId() == null) {
                order.setSellerTaxRegistrationId(taxContext.sellerTaxRegistrationId());
                order.setSellerGstin(taxContext.sellerGstin());
                order.setPlaceOfSupplyStateCode(taxContext.placeOfSupplyStateCode());
            }

            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setPurchaseOrderId(order.getId());
            line.setProductId(product.getId());
            line.setSupplierProductId(supplierProduct.getId());
            line.setProductMasterId(product.getProductId());
            line.setSkuSnapshot(product.getSku());
            line.setProductNameSnapshot(product.getName());
            line.setHsnSnapshot(productMaster.getHsnCode());
            line.setSupplierProductCodeSnapshot(supplierProduct.getSupplierProductCode());
            line.setUomId(reqLine.uomId());
            line.setQuantity(reqLine.quantity());
            line.setBaseQuantity(reqLine.baseQuantity());
            line.setUnitPrice(reqLine.unitPrice());
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
            line.setReceivedBaseQuantity(BigDecimal.ZERO);
            purchaseOrderLineRepository.save(line);

            subtotal = subtotal.add(lineBase);
            taxAmount = taxAmount.add(lineTax);
            total = total.add(lineTotal);
        }

        order.setSubtotal(subtotal);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(total);

        ErpApprovalService.ApprovalEvaluation approvalEvaluation = approvalService.evaluate(
                organizationId,
                new ErpApprovalDtos.ApprovalEvaluationQuery("purchase_order", order.getId(), "PURCHASE_ORDER_CREATE")
        );
        if (approvalEvaluation.approvalRequired()) {
            order.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
            order.setApprovedAt(null);
            order.setApprovedBy(null);
            purchaseOrderRepository.save(order);

            if (!approvalEvaluation.pendingRequestExists()) {
                approvalService.createRequest(
                        organizationId,
                        branchId,
                        new ErpApprovalDtos.CreateApprovalRequest(
                                "purchase_order",
                                order.getId(),
                                order.getPoNumber(),
                                approvalEvaluation.approvalType(),
                                "Purchase order amount matched approval rule",
                                null,
                                approvalEvaluation.approverRoleCode()
                        )
                );
            }
        } else {
            order.setStatus(ErpDocumentStatuses.APPROVED);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(ErpSecurityUtils.currentUserId().orElse(1L));
            purchaseOrderRepository.save(order);
        }
        order = purchaseOrderRepository.findById(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found after save: " + poNumber));

        auditEventWriter.write(
                organizationId, branchId, "PURCHASE_ORDER_POSTED", "purchase_order", order.getId(), order.getPoNumber(),
                approvalEvaluation.approvalRequired() ? "REQUEST_APPROVAL" : "POST",
                null, null, order.getSupplierId(),
                approvalEvaluation.approvalRequired() ? "Purchase order created and sent for approval" : "Purchase order created",
                ErpJsonPayloads.of(
                        "poNumber", order.getPoNumber(),
                        "poId", order.getId(),
                        "total", order.getTotalAmount(),
                        "status", order.getStatus()
                )
        );
        List<PurchaseOrderLine> savedLines = purchaseOrderLineRepository.findByPurchaseOrderIdOrderByIdAsc(order.getId());
        return toPurchaseOrderResponse(order, savedLines);
    }

    @Transactional(readOnly = true)
    public List<PurchaseReceipt> listPurchaseReceipts(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "purchases");
        return purchaseReceiptRepository.findTop100ByOrganizationIdOrderByReceiptDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public ErpPurchaseResponses.PurchaseReceiptResponse getPurchaseReceipt(Long id) {
        PurchaseReceipt receipt = purchaseReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase receipt not found: " + id));
        accessGuard.assertOrganizationAccess(receipt.getOrganizationId());
        accessGuard.assertBranchAccess(receipt.getOrganizationId(), receipt.getBranchId());
        subscriptionAccessService.assertFeature(receipt.getOrganizationId(), "purchases");
        List<PurchaseReceiptLine> lines = purchaseReceiptLineRepository.findByPurchaseReceiptIdOrderByIdAsc(id);
        return toPurchaseReceiptResponse(receipt, lines);
    }

    public ErpPurchaseResponses.PurchaseReceiptResponse createPurchaseReceipt(Long organizationId, Long branchId, ErpPurchaseDtos.CreatePurchaseReceiptRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "purchases");
        var supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));
        ensureSupplierBelongsToOrganization(organizationId, supplier.getId());

        String receiptNumber = "PRC-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();

        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setOrganizationId(organizationId);
        receipt.setBranchId(branchId);
        receipt.setWarehouseId(request.warehouseId());
        receipt.setPurchaseOrderId(request.purchaseOrderId());
        receipt.setSupplierId(request.supplierId());
        receipt.setReceiptNumber(receiptNumber);
        receipt.setReceiptDate(request.receiptDate() == null ? LocalDate.now() : request.receiptDate());
        receipt.setDueDate(request.dueDate() == null ? receipt.getReceiptDate() : request.dueDate());
        receipt.setSupplierGstin(supplier.getGstin());
        receipt.setPlaceOfSupplyStateCode(request.placeOfSupplyStateCode());
        receipt.setStatus(ErpDocumentStatuses.POSTED);
        receipt.setRemarks(request.remarks());
        receipt.setPostedAt(LocalDateTime.now());
        receipt = purchaseReceiptRepository.save(receipt);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (ErpPurchaseDtos.CreatePurchaseReceiptLineRequest reqLine : request.lines()) {
            StoreProduct product = productRepository.findById(reqLine.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + reqLine.productId()));
            ensureStoreProductBelongsToOrganization(organizationId, product);
            Product productMaster = productMasterRepository.findById(product.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product master not found: " + product.getProductId()));
            SupplierProduct supplierProduct = resolveSupplierProduct(organizationId, supplier.getId(), product.getProductId(), reqLine.supplierProductId());
            uomRepository.findById(reqLine.uomId())
                    .orElseThrow(() -> new ResourceNotFoundException("UOM not found: " + reqLine.uomId()));
            validateReceiptTracking(product, reqLine);

            BigDecimal lineBase = reqLine.unitCost().multiply(reqLine.quantity());
            GstTaxService.TaxContext taxContext = gstTaxService.resolvePurchaseTax(
                    organizationId,
                    branchId,
                    receipt.getReceiptDate(),
                    product.getTaxGroupId(),
                    supplier.getGstin(),
                    request.placeOfSupplyStateCode(),
                    lineBase
            );
            BigDecimal lineTax = taxContext.totalTaxAmount();
            BigDecimal lineTotal = taxContext.lineTotal();

            if (receipt.getSellerTaxRegistrationId() == null) {
                receipt.setSellerTaxRegistrationId(taxContext.sellerTaxRegistrationId());
                receipt.setSellerGstin(taxContext.sellerGstin());
                receipt.setPlaceOfSupplyStateCode(taxContext.placeOfSupplyStateCode());
            }

            PurchaseReceiptLine line = new PurchaseReceiptLine();
            line.setPurchaseReceiptId(receipt.getId());
            line.setPurchaseOrderLineId(reqLine.purchaseOrderLineId());
            line.setProductId(product.getId());
            line.setSupplierProductId(supplierProduct.getId());
            line.setProductMasterId(product.getProductId());
            line.setSkuSnapshot(product.getSku());
            line.setProductNameSnapshot(product.getName());
            line.setHsnSnapshot(productMaster.getHsnCode());
            line.setSupplierProductCodeSnapshot(supplierProduct.getSupplierProductCode());
            line.setUomId(reqLine.uomId());
            line.setQuantity(reqLine.quantity());
            line.setBaseQuantity(reqLine.baseQuantity());
            line.setUnitCost(reqLine.unitCost());
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
            line = purchaseReceiptLineRepository.save(line);

            if (reqLine.purchaseOrderLineId() != null) {
                PurchaseOrderLine poLine = purchaseOrderLineRepository.findById(reqLine.purchaseOrderLineId())
                        .orElseThrow(() -> new ResourceNotFoundException("Purchase order line not found: " + reqLine.purchaseOrderLineId()));
                poLine.setReceivedBaseQuantity(poLine.getReceivedBaseQuantity().add(reqLine.baseQuantity()));
                purchaseOrderLineRepository.save(poLine);
            }

            if (Boolean.TRUE.equals(product.getSerialTrackingEnabled()) && reqLine.serialNumbers() != null && !reqLine.serialNumbers().isEmpty()) {
                if (BigDecimal.valueOf(reqLine.serialNumbers().size()).compareTo(reqLine.baseQuantity()) != 0) {
                    throw new BusinessException("Serialized receipt requires serial count equal to base quantity for product " + reqLine.productId());
                }
                for (String serialValue : reqLine.serialNumbers()) {
                    serialNumberRepository.findFirstByOrganizationIdAndSerialNumberIgnoreCase(organizationId, serialValue)
                            .ifPresent(existing -> {
                                throw new BusinessException("Serial already exists in organization: " + serialValue);
                            });
                    SerialNumber serial = new SerialNumber();
                    serial.setOrganizationId(organizationId);
                    serial.setProductId(product.getId());
                    serial.setSerialNumber(serialValue);
                    serial.setManufacturerSerialNumber(serialValue);
                    serial.setStatus(ErpDocumentStatuses.IN_STOCK);
                    serial.setCurrentWarehouseId(receipt.getWarehouseId());
                    serial = serialNumberRepository.save(serial);

                    PurchaseReceiptLineSerial link = new PurchaseReceiptLineSerial();
                    link.setPurchaseReceiptLineId(line.getId());
                    link.setSerialNumberId(serial.getId());
                    purchaseReceiptLineSerialRepository.save(link);

                    inventoryPostingService.postMovement(
                            organizationId, branchId, receipt.getWarehouseId(), product.getId(), null,
                            reqLine.uomId(), BigDecimal.ONE, BigDecimal.ONE, "IN", ErpInventoryMovementTypes.PURCHASE_RECEIPT,
                            "purchase_receipt", receipt.getId(), receiptNumber, reqLine.unitCost(),
                            payloadJson(receipt, product.getId(), line.getId(), "SERIAL")
                    );
                }
            } else if (reqLine.batchEntries() != null && !reqLine.batchEntries().isEmpty()) {
                BigDecimal batchBase = BigDecimal.ZERO;
                for (ErpPurchaseDtos.CreateBatchReceiptLine batchEntry : reqLine.batchEntries()) {
                    InventoryBatch batch = new InventoryBatch();
                    batch.setOrganizationId(organizationId);
                    batch.setProductId(product.getId());
                    batch.setBatchNumber(batchEntry.batchNumber() == null || batchEntry.batchNumber().isBlank()
                            ? "BATCH-" + System.currentTimeMillis() : batchEntry.batchNumber());
                    batch.setManufacturerBatchNumber(batchEntry.manufacturerBatchNumber());
                    batch.setManufacturedOn(batchEntry.manufacturedOn());
                    batch.setExpiryOn(batchEntry.expiryOn());
                    batch.setStatus(ErpDocumentStatuses.ACTIVE);
                    batch = inventoryBatchRepository.save(batch);

                    PurchaseReceiptLineBatch link = new PurchaseReceiptLineBatch();
                    link.setPurchaseReceiptLineId(line.getId());
                    link.setBatchId(batch.getId());
                    link.setQuantity(batchEntry.quantity());
                    link.setBaseQuantity(batchEntry.baseQuantity());
                    purchaseReceiptLineBatchRepository.save(link);

                    inventoryPostingService.postMovement(
                            organizationId, branchId, receipt.getWarehouseId(), product.getId(), batch.getId(),
                            reqLine.uomId(), batchEntry.quantity(), batchEntry.baseQuantity(), "IN", ErpInventoryMovementTypes.PURCHASE_RECEIPT,
                            "purchase_receipt", receipt.getId(), receiptNumber, reqLine.unitCost(),
                            payloadJson(receipt, product.getId(), line.getId(), "BATCH")
                    );
                    batchBase = batchBase.add(batchEntry.baseQuantity());
                }
                if (batchBase.compareTo(reqLine.baseQuantity()) != 0) {
                    throw new BusinessException("Batch base quantity mismatch for product " + reqLine.productId());
                }
            } else {
                inventoryPostingService.postMovement(
                        organizationId, branchId, receipt.getWarehouseId(), product.getId(), null,
                        reqLine.uomId(), reqLine.quantity(), reqLine.baseQuantity(), "IN", ErpInventoryMovementTypes.PURCHASE_RECEIPT,
                        "purchase_receipt", receipt.getId(), receiptNumber, reqLine.unitCost(),
                        payloadJson(receipt, product.getId(), line.getId(), "STANDARD")
                );
            }

            subtotal = subtotal.add(lineBase);
            taxAmount = taxAmount.add(lineTax);
            total = total.add(lineTotal);
        }

        receipt.setSubtotal(subtotal);
        receipt.setTaxAmount(taxAmount);
        receipt.setTotalAmount(total);
        receipt = purchaseReceiptRepository.save(receipt);
        accountingPostingService.postPurchaseReceipt(receipt);

        if (receipt.getPurchaseOrderId() != null) {
            Long purchaseOrderId = receipt.getPurchaseOrderId();
            List<PurchaseOrderLine> poLines = purchaseOrderLineRepository.findByPurchaseOrderIdOrderByIdAsc(purchaseOrderId);
            boolean allReceived = poLines.stream().allMatch(l -> l.getReceivedBaseQuantity().compareTo(l.getBaseQuantity()) >= 0);
            PurchaseOrder order = purchaseOrderRepository.findById(purchaseOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + purchaseOrderId));
            order.setStatus(allReceived ? ErpDocumentStatuses.RECEIVED : ErpDocumentStatuses.PARTIALLY_RECEIVED);
            purchaseOrderRepository.save(order);
        }

        auditEventWriter.write(
                organizationId, branchId, "PURCHASE_RECEIPT_POSTED", "purchase_receipt", receipt.getId(), receipt.getReceiptNumber(),
                "POST", receipt.getWarehouseId(), null, receipt.getSupplierId(), "Purchase receipt posted",
                ErpJsonPayloads.of(
                        "receiptNumber", receipt.getReceiptNumber(),
                        "receiptId", receipt.getId(),
                        "total", receipt.getTotalAmount()
                )
        );

        List<PurchaseReceiptLine> savedLines = purchaseReceiptLineRepository.findByPurchaseReceiptIdOrderByIdAsc(receipt.getId());
        return toPurchaseReceiptResponse(receipt, savedLines);
    }

    @Transactional(readOnly = true)
    public List<SupplierPayment> listSupplierPayments(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "payments");
        return supplierPaymentRepository.findTop100ByOrganizationIdOrderByPaymentDateDescIdDesc(organizationId);
    }

    public SupplierPayment createSupplierPayment(Long organizationId, Long branchId, ErpPurchaseDtos.CreateSupplierPaymentRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "payments");
        supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));

        String paymentNumber = "SPY-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();

        SupplierPayment payment = new SupplierPayment();
        payment.setOrganizationId(organizationId);
        payment.setBranchId(branchId);
        payment.setSupplierId(request.supplierId());
        payment.setPaymentNumber(paymentNumber);
        payment.setPaymentDate(request.paymentDate() == null ? LocalDate.now() : request.paymentDate());
        payment.setPaymentMethod(request.paymentMethod().toUpperCase());
        payment.setReferenceNumber(request.referenceNumber());
        payment.setAmount(request.amount());
        payment.setStatus(ErpDocumentStatuses.POSTED);
       payment.setRemarks(request.remarks());
        payment = supplierPaymentRepository.save(payment);
        accountingPostingService.postSupplierPayment(payment);

        auditEventWriter.write(
                organizationId, branchId, "SUPPLIER_PAYMENT_POSTED", "supplier_payment", payment.getId(), payment.getPaymentNumber(),
                "POST", null, null, payment.getSupplierId(), "Supplier payment posted",
                ErpJsonPayloads.of(
                        "paymentNumber", payment.getPaymentNumber(),
                        "amount", payment.getAmount()
                )
        );

        return payment;
    }

    public SupplierPayment allocateSupplierPayment(Long paymentId, ErpPurchaseDtos.AllocateSupplierPaymentRequest request) {
        SupplierPayment payment = supplierPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier payment not found: " + paymentId));
        accessGuard.assertOrganizationAccess(payment.getOrganizationId());
        accessGuard.assertBranchAccess(payment.getOrganizationId(), payment.getBranchId());
        subscriptionAccessService.assertFeature(payment.getOrganizationId(), "payments");
        if (ErpDocumentStatuses.CANCELLED.equals(payment.getStatus())) {
            throw new BusinessException("Cannot allocate a cancelled supplier payment");
        }

        BigDecimal totalAllocated = BigDecimal.ZERO;
        for (ErpPurchaseDtos.SupplierPaymentAllocationLine line : request.allocations()) {
            PurchaseReceipt receipt = purchaseReceiptRepository.findById(line.purchaseReceiptId())
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase receipt not found: " + line.purchaseReceiptId()));
            if (!payment.getOrganizationId().equals(receipt.getOrganizationId())) {
                throw new BusinessException("Purchase receipt does not belong to the payment organization");
            }

            SupplierPaymentAllocation allocation = new SupplierPaymentAllocation();
            allocation.setSupplierPaymentId(payment.getId());
            allocation.setPurchaseReceiptId(receipt.getId());
            allocation.setAllocatedAmount(line.allocatedAmount());
            supplierPaymentAllocationRepository.save(allocation);
            totalAllocated = totalAllocated.add(line.allocatedAmount());

            BigDecimal receiptAllocated = supplierPaymentAllocationRepository.findByPurchaseReceiptId(receipt.getId()).stream()
                    .map(SupplierPaymentAllocation::getAllocatedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (receiptAllocated.compareTo(receipt.getTotalAmount()) >= 0) {
                receipt.setStatus(ErpDocumentStatuses.BILLED);
            } else if (receiptAllocated.compareTo(BigDecimal.ZERO) > 0) {
                receipt.setStatus(ErpDocumentStatuses.PARTIALLY_BILLED);
            }
            purchaseReceiptRepository.save(receipt);
        }

        if (totalAllocated.compareTo(payment.getAmount()) > 0) {
            throw new BusinessException("Allocated amount cannot exceed supplier payment amount");
        }
        if (totalAllocated.compareTo(BigDecimal.ZERO) > 0) {
            payment.setStatus(ErpDocumentStatuses.ALLOCATED);
            supplierPaymentRepository.save(payment);
        }

        auditEventWriter.write(
                payment.getOrganizationId(), payment.getBranchId(), "SUPPLIER_PAYMENT_ALLOCATED", "supplier_payment", payment.getId(),
                payment.getPaymentNumber(), "ALLOCATE", null, null, payment.getSupplierId(), "Supplier payment allocated",
                ErpJsonPayloads.of(
                        "paymentNumber", payment.getPaymentNumber(),
                        "allocated", totalAllocated
                )
        );

        return payment;
    }

    private String payloadJson(PurchaseReceipt receipt, Long productId, Long lineId, String mode) {
        return ErpJsonPayloads.of(
                "receiptId", receipt.getId(),
                "receiptNumber", receipt.getReceiptNumber(),
                "productId", productId,
                "lineId", lineId,
                "mode", mode
        );
    }

    private void validateReceiptTracking(StoreProduct product, ErpPurchaseDtos.CreatePurchaseReceiptLineRequest reqLine) {
        boolean expectsSerials = Boolean.TRUE.equals(product.getSerialTrackingEnabled());
        boolean expectsBatches = Boolean.TRUE.equals(product.getBatchTrackingEnabled());
        boolean hasSerials = reqLine.serialNumbers() != null && !reqLine.serialNumbers().isEmpty();
        boolean hasBatches = reqLine.batchEntries() != null && !reqLine.batchEntries().isEmpty();

        if (expectsSerials) {
            if (!hasSerials) {
                throw new BusinessException("Serialized product requires serial numbers on receipt: " + product.getSku());
            }
            if (hasBatches) {
                throw new BusinessException("Serialized product cannot receive batch entries: " + product.getSku());
            }
            if (reqLine.baseQuantity().stripTrailingZeros().scale() > 0) {
                throw new BusinessException("Serialized product base quantity must be a whole number: " + product.getSku());
            }
            return;
        }

        if (expectsBatches) {
            if (!hasBatches) {
                throw new BusinessException("Batch-tracked product requires batch entries on receipt: " + product.getSku());
            }
            if (hasSerials) {
                throw new BusinessException("Batch-tracked product cannot receive serial numbers: " + product.getSku());
            }
            return;
        }

        if (hasSerials || hasBatches) {
            throw new BusinessException("Standard product does not accept serial or batch details: " + product.getSku());
        }
    }

    private ErpPurchaseResponses.PurchaseOrderResponse toPurchaseOrderResponse(PurchaseOrder order, List<PurchaseOrderLine> lines) {
        return new ErpPurchaseResponses.PurchaseOrderResponse(
                order.getId(),
                order.getOrganizationId(),
                order.getBranchId(),
                order.getSupplierId(),
                order.getPoNumber(),
                order.getPoDate(),
                order.getSellerGstin(),
                order.getSupplierGstin(),
                order.getPlaceOfSupplyStateCode(),
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getStatus(),
                lines.stream().map(this::toPurchaseLineResponseFromOrder).toList()
        );
    }

    private ErpPurchaseResponses.PurchaseReceiptResponse toPurchaseReceiptResponse(PurchaseReceipt receipt, List<PurchaseReceiptLine> lines) {
        return new ErpPurchaseResponses.PurchaseReceiptResponse(
                receipt.getId(),
                receipt.getOrganizationId(),
                receipt.getBranchId(),
                receipt.getWarehouseId(),
                receipt.getSupplierId(),
                receipt.getReceiptNumber(),
                receipt.getReceiptDate(),
                receipt.getDueDate(),
                receipt.getSellerGstin(),
                receipt.getSupplierGstin(),
                receipt.getPlaceOfSupplyStateCode(),
                receipt.getSubtotal(),
                receipt.getTaxAmount(),
                receipt.getTotalAmount(),
                allocatedAmount(receipt.getId()),
                outstandingAmount(receipt.getId(), receipt.getTotalAmount()),
                receipt.getStatus(),
                lines.stream().map(this::toPurchaseLineResponseFromReceipt).toList()
        );
    }

    private ErpPurchaseResponses.PurchaseLineResponse toPurchaseLineResponseFromOrder(PurchaseOrderLine line) {
        return new ErpPurchaseResponses.PurchaseLineResponse(
                line.getId(),
                line.getProductId(),
                line.getSupplierProductId(),
                line.getProductMasterId(),
                line.getSkuSnapshot(),
                line.getProductNameSnapshot(),
                line.getHsnSnapshot(),
                line.getSupplierProductCodeSnapshot(),
                line.getUomId(),
                line.getQuantity(),
                line.getBaseQuantity(),
                line.getUnitPrice(),
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
        );
    }

    private ErpPurchaseResponses.PurchaseLineResponse toPurchaseLineResponseFromReceipt(PurchaseReceiptLine line) {
        return new ErpPurchaseResponses.PurchaseLineResponse(
                line.getId(),
                line.getProductId(),
                line.getSupplierProductId(),
                line.getProductMasterId(),
                line.getSkuSnapshot(),
                line.getProductNameSnapshot(),
                line.getHsnSnapshot(),
                line.getSupplierProductCodeSnapshot(),
                line.getUomId(),
                line.getQuantity(),
                line.getBaseQuantity(),
                line.getUnitCost(),
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
        );
    }

    private BigDecimal allocatedAmount(Long purchaseReceiptId) {
        return supplierPaymentAllocationRepository.findByPurchaseReceiptId(purchaseReceiptId).stream()
                .map(SupplierPaymentAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal outstandingAmount(Long purchaseReceiptId, BigDecimal totalAmount) {
        return totalAmount.subtract(allocatedAmount(purchaseReceiptId)).max(BigDecimal.ZERO);
    }

    private void ensureSupplierBelongsToOrganization(Long organizationId, Long supplierId) {
        supplierRepository.findByIdAndOrganizationId(supplierId, organizationId)
                .orElseThrow(() -> new BusinessException("Supplier does not belong to organization " + organizationId + ": " + supplierId));
        storeSupplierTermsRepository.findByOrganizationIdAndSupplierId(organizationId, supplierId)
                .filter(terms -> Boolean.TRUE.equals(terms.getIsActive()))
                .orElseThrow(() -> new BusinessException("Supplier terms are not active for supplier " + supplierId));
    }

    private void ensureStoreProductBelongsToOrganization(Long organizationId, StoreProduct product) {
        if (!organizationId.equals(product.getOrganizationId())) {
            throw new BusinessException("Store product does not belong to organization " + organizationId + ": " + product.getId());
        }
    }

    private SupplierProduct resolveSupplierProduct(Long organizationId, Long supplierId, Long productMasterId, Long supplierProductId) {
        if (supplierProductId != null) {
            SupplierProduct supplierProduct = supplierProductRepository.findByIdAndOrganizationId(supplierProductId, organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier product not found: " + supplierProductId));
            if (!supplierId.equals(supplierProduct.getSupplierId())) {
                throw new BusinessException("Supplier product does not belong to supplier " + supplierId + ": " + supplierProductId);
            }
            if (!productMasterId.equals(supplierProduct.getProductId())) {
                throw new BusinessException("Supplier product does not match selected product for supplier " + supplierId);
            }
            if (!Boolean.TRUE.equals(supplierProduct.getIsActive())) {
                throw new BusinessException("Supplier product is inactive: " + supplierProductId);
            }
            return supplierProduct;
        }

        List<SupplierProduct> matches = supplierProductRepository
                .findByOrganizationIdAndSupplierIdAndProductIdAndIsActiveTrueOrderByIsPreferredDescPriorityAscIdAsc(
                        organizationId, supplierId, productMasterId
                );
        if (matches.isEmpty()) {
            throw new BusinessException("Selected supplier does not supply this product yet. Link the supplier to the product first.");
        }
        if (matches.size() > 1) {
            throw new BusinessException("Multiple supplier product mappings exist. Provide supplierProductId to choose the correct one.");
        }
        return matches.getFirst();
    }
}
