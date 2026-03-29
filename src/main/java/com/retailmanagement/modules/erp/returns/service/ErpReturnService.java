package com.retailmanagement.modules.erp.returns.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.constants.ErpInventoryMovementTypes;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.finance.service.ErpAccountingPostingService;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBatchRepository;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.inventory.service.InventoryPostingService;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceipt;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceiptLine;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceiptLineBatch;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceiptLineSerial;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseReceiptLineBatchRepository;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseReceiptLineRepository;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseReceiptLineSerialRepository;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseReceiptRepository;
import com.retailmanagement.modules.erp.returns.dto.ErpReturnDtos;
import com.retailmanagement.modules.erp.returns.dto.ErpReturnResponses;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturn;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLineBatch;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLineSerial;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLine;
import com.retailmanagement.modules.erp.returns.entity.SalesReturn;
import com.retailmanagement.modules.erp.returns.entity.SalesReturnLineBatch;
import com.retailmanagement.modules.erp.returns.entity.SalesReturnLineSerial;
import com.retailmanagement.modules.erp.returns.entity.SalesReturnLine;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnLineBatchRepository;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnLineSerialRepository;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnLineRepository;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnLineBatchRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnLineSerialRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnLineRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnRepository;
import com.retailmanagement.modules.erp.sales.entity.ProductOwnership;
import com.retailmanagement.modules.erp.sales.entity.SalesLineBatch;
import com.retailmanagement.modules.erp.sales.entity.SalesLineSerial;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.repository.ProductOwnershipRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesLineBatchRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesLineSerialRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ErpReturnService {

    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnLineRepository salesReturnLineRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseReturnLineRepository purchaseReturnLineRepository;
    private final SalesReturnLineSerialRepository salesReturnLineSerialRepository;
    private final SalesReturnLineBatchRepository salesReturnLineBatchRepository;
    private final PurchaseReturnLineSerialRepository purchaseReturnLineSerialRepository;
    private final PurchaseReturnLineBatchRepository purchaseReturnLineBatchRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SalesInvoiceLineRepository salesInvoiceLineRepository;
    private final SalesLineSerialRepository salesLineSerialRepository;
    private final SalesLineBatchRepository salesLineBatchRepository;
    private final ProductOwnershipRepository productOwnershipRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final PurchaseReceiptLineRepository purchaseReceiptLineRepository;
    private final PurchaseReceiptLineSerialRepository purchaseReceiptLineSerialRepository;
    private final PurchaseReceiptLineBatchRepository purchaseReceiptLineBatchRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryPostingService inventoryPostingService;
    private final ErpAccountingPostingService accountingPostingService;
    private final AuditEventWriter auditEventWriter;
    private final ErpAccessGuard accessGuard;
    private final SubscriptionAccessService subscriptionAccessService;

    @Transactional(readOnly = true)
    public List<SalesReturn> listSalesReturns(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        return salesReturnRepository.findTop100ByOrganizationIdOrderByReturnDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public List<PurchaseReturn> listPurchaseReturns(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "purchases");
        return purchaseReturnRepository.findTop100ByOrganizationIdOrderByReturnDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public ErpReturnResponses.SalesReturnResponse getSalesReturn(Long id) {
        SalesReturn header = salesReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales return not found: " + id));
        accessGuard.assertBranchAccess(header.getOrganizationId(), header.getBranchId());
        List<SalesReturnLine> lines = salesReturnLineRepository.findBySalesReturnIdOrderByIdAsc(id);
        return toSalesReturnResponse(header, lines);
    }

    @Transactional(readOnly = true)
    public ErpReturnResponses.PurchaseReturnResponse getPurchaseReturn(Long id) {
        PurchaseReturn header = purchaseReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase return not found: " + id));
        accessGuard.assertBranchAccess(header.getOrganizationId(), header.getBranchId());
        List<PurchaseReturnLine> lines = purchaseReturnLineRepository.findByPurchaseReturnIdOrderByIdAsc(id);
        return toPurchaseReturnResponse(header, lines);
    }

    public ErpReturnResponses.SalesReturnResponse createSalesReturn(Long organizationId, Long branchId, ErpReturnDtos.CreateSalesReturnRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        SalesInvoice invoice = salesInvoiceRepository.findByOrganizationIdAndId(organizationId, request.originalSalesInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + request.originalSalesInvoiceId()));

        SalesReturn header = new SalesReturn();
        header.setOrganizationId(organizationId);
        header.setBranchId(branchId);
        header.setWarehouseId(invoice.getWarehouseId());
        header.setCustomerId(invoice.getCustomerId());
        header.setOriginalSalesInvoiceId(invoice.getId());
        header.setReturnNumber("SRN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis());
        header.setReturnDate(request.returnDate() == null ? LocalDate.now() : request.returnDate());
        header.setSellerGstin(invoice.getSellerGstin());
        header.setCustomerGstin(invoice.getCustomerGstin());
        header.setPlaceOfSupplyStateCode(invoice.getPlaceOfSupplyStateCode());
        header.setReason(request.reason());
        header.setRemarks(request.remarks());
        header.setStatus("PENDING_INSPECTION");
        header = salesReturnRepository.save(header);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal inventoryValue = BigDecimal.ZERO;

        for (ErpReturnDtos.CreateSalesReturnLineRequest reqLine : request.lines()) {
            SalesInvoiceLine original = salesInvoiceLineRepository.findById(reqLine.originalSalesInvoiceLineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice line not found: " + reqLine.originalSalesInvoiceLineId()));
            if (!invoice.getId().equals(original.getSalesInvoiceId())) {
                throw new BusinessException("Sales return line does not belong to the selected invoice");
            }
            BigDecimal previouslyReturned = salesReturnLineRepository.findByOriginalSalesInvoiceLineId(original.getId()).stream()
                    .map(SalesReturnLine::getBaseQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = original.getBaseQuantity().subtract(previouslyReturned);
            if (reqLine.baseQuantity().compareTo(remaining) > 0) {
                throw new BusinessException("Return quantity exceeds remaining quantity for invoice line " + original.getId());
            }

            BigDecimal ratio = reqLine.baseQuantity().divide(original.getBaseQuantity(), 8, RoundingMode.HALF_UP);
            SalesReturnLine line = new SalesReturnLine();
            line.setSalesReturnId(header.getId());
            line.setOriginalSalesInvoiceLineId(original.getId());
            line.setProductId(original.getProductId());
            line.setUomId(original.getUomId());
            line.setQuantity(reqLine.quantity());
            line.setBaseQuantity(reqLine.baseQuantity());
            line.setUnitPrice(original.getUnitPrice());
            line.setDiscountAmount(proportional(original.getDiscountAmount(), ratio));
            line.setTaxableAmount(proportional(original.getTaxableAmount(), ratio));
            line.setTaxRate(original.getTaxRate());
            line.setCgstRate(original.getCgstRate());
            line.setCgstAmount(proportional(original.getCgstAmount(), ratio));
            line.setSgstRate(original.getSgstRate());
            line.setSgstAmount(proportional(original.getSgstAmount(), ratio));
            line.setIgstRate(original.getIgstRate());
            line.setIgstAmount(proportional(original.getIgstAmount(), ratio));
            line.setCessRate(original.getCessRate());
            line.setCessAmount(proportional(original.getCessAmount(), ratio));
            line.setLineAmount(proportional(original.getLineAmount(), ratio));
            line.setTotalCostAtReturn(proportional(original.getTotalCostAtSale(), ratio));
            line.setUnitCostAtReturn(reqLine.baseQuantity().compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : line.getTotalCostAtReturn().divide(reqLine.baseQuantity(), 2, RoundingMode.HALF_UP));
            line.setDisposition(normalizeDisposition(reqLine.disposition()));
            line.setReason(reqLine.reason());
            line.setInspectionStatus("PENDING");
            line = salesReturnLineRepository.save(line);

            List<SalesLineSerial> originalSerials = salesLineSerialRepository.findBySalesInvoiceLineId(original.getId());
            List<SalesLineBatch> originalBatches = salesLineBatchRepository.findBySalesInvoiceLineId(original.getId());
            validateReturnTracking(reqLine.serialNumberIds(), reqLine.batchSelections(), originalSerials, originalBatches, reqLine.baseQuantity(), "sales invoice line " + original.getId());

            subtotal = subtotal.add(line.getTaxableAmount());
            taxAmount = taxAmount.add(line.getCgstAmount()).add(line.getSgstAmount()).add(line.getIgstAmount()).add(line.getCessAmount());
            totalAmount = totalAmount.add(line.getLineAmount());
        }

        header.setSubtotal(subtotal);
        header.setTaxAmount(taxAmount);
        header.setTotalAmount(totalAmount);
        header = salesReturnRepository.save(header);
        auditEventWriter.write(
                organizationId, branchId, "SALES_RETURN_CREATED", "sales_return", header.getId(), header.getReturnNumber(),
                "CREATE", header.getWarehouseId(), header.getCustomerId(), null, "Sales return created pending inspection",
                ErpJsonPayloads.of("returnId", header.getId(), "originalSalesInvoiceId", header.getOriginalSalesInvoiceId(), "total", header.getTotalAmount())
        );

        return toSalesReturnResponse(header, salesReturnLineRepository.findBySalesReturnIdOrderByIdAsc(header.getId()));
    }

    public ErpReturnResponses.SalesReturnResponse inspectSalesReturn(Long id, ErpReturnDtos.InspectSalesReturnRequest request) {
        SalesReturn header = salesReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales return not found: " + id));
        accessGuard.assertBranchAccess(header.getOrganizationId(), header.getBranchId());
        if (!"PENDING_INSPECTION".equals(header.getStatus())) {
            throw new BusinessException("Sales return is not pending inspection: " + id);
        }

        Map<Long, ErpReturnDtos.InspectSalesReturnLineRequest> inspectionByLineId = new HashMap<>();
        for (ErpReturnDtos.InspectSalesReturnLineRequest lineRequest : request.lines()) {
            inspectionByLineId.put(lineRequest.salesReturnLineId(), lineRequest);
        }

        List<SalesReturnLine> lines = salesReturnLineRepository.findBySalesReturnIdOrderByIdAsc(id);
        BigDecimal inventoryValue = BigDecimal.ZERO;
        Long originalSalesInvoiceId = header.getOriginalSalesInvoiceId();
        SalesInvoice invoice = salesInvoiceRepository.findById(originalSalesInvoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + originalSalesInvoiceId));

        for (SalesReturnLine line : lines) {
            ErpReturnDtos.InspectSalesReturnLineRequest lineRequest = inspectionByLineId.get(line.getId());
            if (lineRequest == null) {
                throw new BusinessException("Inspection details missing for sales return line " + line.getId());
            }
            line.setDisposition(normalizeDisposition(lineRequest.disposition() == null ? line.getDisposition() : lineRequest.disposition()));
            line.setInspectionStatus(normalizeInspectionStatus(lineRequest.inspectionStatus()));
            line.setInspectionNotes(lineRequest.inspectionNotes());
            salesReturnLineRepository.save(line);

            if (!"APPROVED".equals(line.getInspectionStatus())) {
                continue;
            }

            SalesInvoiceLine original = salesInvoiceLineRepository.findById(line.getOriginalSalesInvoiceLineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice line not found: " + line.getOriginalSalesInvoiceLineId()));
            List<SalesLineSerial> originalSerials = salesLineSerialRepository.findBySalesInvoiceLineId(original.getId());
            List<SalesLineBatch> originalBatches = salesLineBatchRepository.findBySalesInvoiceLineId(original.getId());

            if ("RESTOCK".equals(line.getDisposition())) {
                if (!originalSerials.isEmpty()) {
                    postApprovedSalesReturnSerials(header, line, original, invoice);
                } else if (!originalBatches.isEmpty()) {
                    postApprovedSalesReturnBatches(header, line, invoice);
                } else {
                    inventoryPostingService.postMovement(
                            header.getOrganizationId(),
                            header.getBranchId(),
                            header.getWarehouseId(),
                            line.getProductId(),
                            null,
                            line.getUomId(),
                            line.getQuantity(),
                            line.getBaseQuantity(),
                            "IN",
                            ErpInventoryMovementTypes.SALES_RETURN,
                            "sales_return",
                            header.getId(),
                            header.getReturnNumber(),
                            line.getUnitCostAtReturn(),
                            ErpJsonPayloads.of("salesReturnId", header.getId(), "lineId", line.getId(), "sourceInvoiceId", invoice.getId())
                    );
                }
                inventoryValue = inventoryValue.add(line.getTotalCostAtReturn());
            }
        }

        header.setStatus(ErpDocumentStatuses.POSTED);
        header.setInspectedAt(LocalDateTime.now());
        header.setInspectedBy(com.retailmanagement.modules.erp.common.ErpSecurityUtils.currentUserId().orElse(1L));
        header.setInspectionNotes(request.inspectionNotes());
        header.setPostedAt(LocalDateTime.now());
        header = salesReturnRepository.save(header);
        accountingPostingService.postSalesReturn(header, inventoryValue);

        auditEventWriter.write(
                header.getOrganizationId(), header.getBranchId(), "SALES_RETURN_POSTED", "sales_return", header.getId(), header.getReturnNumber(),
                "INSPECT_AND_POST", header.getWarehouseId(), header.getCustomerId(), null, "Sales return inspected and posted",
                ErpJsonPayloads.of("returnId", header.getId(), "originalSalesInvoiceId", header.getOriginalSalesInvoiceId(), "total", header.getTotalAmount())
        );

        return toSalesReturnResponse(header, salesReturnLineRepository.findBySalesReturnIdOrderByIdAsc(header.getId()));
    }

    public ErpReturnResponses.PurchaseReturnResponse createPurchaseReturn(Long organizationId, Long branchId, ErpReturnDtos.CreatePurchaseReturnRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "purchases");
        PurchaseReceipt receipt = purchaseReceiptRepository.findById(request.originalPurchaseReceiptId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase receipt not found: " + request.originalPurchaseReceiptId()));
        if (!organizationId.equals(receipt.getOrganizationId())) {
            throw new BusinessException("Purchase receipt does not belong to organization " + organizationId);
        }

        PurchaseReturn header = new PurchaseReturn();
        header.setOrganizationId(organizationId);
        header.setBranchId(branchId);
        header.setWarehouseId(receipt.getWarehouseId());
        header.setSupplierId(receipt.getSupplierId());
        header.setOriginalPurchaseReceiptId(receipt.getId());
        header.setReturnNumber("PRN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis());
        header.setReturnDate(request.returnDate() == null ? LocalDate.now() : request.returnDate());
        header.setSellerGstin(receipt.getSellerGstin());
        header.setSupplierGstin(receipt.getSupplierGstin());
        header.setPlaceOfSupplyStateCode(receipt.getPlaceOfSupplyStateCode());
        header.setReason(request.reason());
        header.setRemarks(request.remarks());
        header.setStatus(ErpDocumentStatuses.POSTED);
        header.setPostedAt(LocalDateTime.now());
        header = purchaseReturnRepository.save(header);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ErpReturnDtos.CreatePurchaseReturnLineRequest reqLine : request.lines()) {
            PurchaseReceiptLine original = purchaseReceiptLineRepository.findById(reqLine.originalPurchaseReceiptLineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase receipt line not found: " + reqLine.originalPurchaseReceiptLineId()));
            if (!receipt.getId().equals(original.getPurchaseReceiptId())) {
                throw new BusinessException("Purchase return line does not belong to the selected receipt");
            }
            BigDecimal previouslyReturned = purchaseReturnLineRepository.findByOriginalPurchaseReceiptLineId(original.getId()).stream()
                    .map(PurchaseReturnLine::getBaseQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = original.getBaseQuantity().subtract(previouslyReturned);
            if (reqLine.baseQuantity().compareTo(remaining) > 0) {
                throw new BusinessException("Return quantity exceeds remaining quantity for receipt line " + original.getId());
            }

            BigDecimal ratio = reqLine.baseQuantity().divide(original.getBaseQuantity(), 8, RoundingMode.HALF_UP);
            PurchaseReturnLine line = new PurchaseReturnLine();
            line.setPurchaseReturnId(header.getId());
            line.setOriginalPurchaseReceiptLineId(original.getId());
            line.setProductId(original.getProductId());
            line.setUomId(original.getUomId());
            line.setQuantity(reqLine.quantity());
            line.setBaseQuantity(reqLine.baseQuantity());
            line.setUnitCost(original.getUnitCost());
            line.setTaxableAmount(proportional(original.getTaxableAmount(), ratio));
            line.setTaxRate(original.getTaxRate());
            line.setCgstRate(original.getCgstRate());
            line.setCgstAmount(proportional(original.getCgstAmount(), ratio));
            line.setSgstRate(original.getSgstRate());
            line.setSgstAmount(proportional(original.getSgstAmount(), ratio));
            line.setIgstRate(original.getIgstRate());
            line.setIgstAmount(proportional(original.getIgstAmount(), ratio));
            line.setCessRate(original.getCessRate());
            line.setCessAmount(proportional(original.getCessAmount(), ratio));
            line.setLineAmount(proportional(original.getLineAmount(), ratio));
            line.setReason(reqLine.reason());
            line = purchaseReturnLineRepository.save(line);

            List<PurchaseReceiptLineSerial> originalSerials = purchaseReceiptLineSerialRepository.findByPurchaseReceiptLineId(original.getId());
            List<PurchaseReceiptLineBatch> originalBatches = purchaseReceiptLineBatchRepository.findByPurchaseReceiptLineId(original.getId());
            validateReturnTracking(reqLine.serialNumberIds(), reqLine.batchSelections(), originalSerials, originalBatches, reqLine.baseQuantity(), "purchase receipt line " + original.getId());

            if (!originalSerials.isEmpty()) {
                for (Long serialId : reqLine.serialNumberIds()) {
                    if (purchaseReturnLineSerialRepository.findBySerialNumberId(serialId).stream().findAny().isPresent()) {
                        throw new BusinessException("Serial " + serialId + " is already linked to a purchase return");
                    }
                    SerialNumber serial = serialNumberRepository.findById(serialId)
                            .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialId));
                    serial.setStatus(ErpDocumentStatuses.RETURNED);
                    serial.setCurrentWarehouseId(null);
                    serialNumberRepository.save(serial);

                    PurchaseReturnLineSerial link = new PurchaseReturnLineSerial();
                    link.setPurchaseReturnLineId(line.getId());
                    link.setSerialNumberId(serialId);
                    purchaseReturnLineSerialRepository.save(link);

                    inventoryPostingService.postMovement(
                            organizationId, branchId, header.getWarehouseId(), line.getProductId(), serial.getBatchId(),
                            line.getUomId(), BigDecimal.ONE, BigDecimal.ONE, "OUT",
                            ErpInventoryMovementTypes.PURCHASE_RETURN, "purchase_return", header.getId(), header.getReturnNumber(),
                            line.getUnitCost(),
                            ErpJsonPayloads.of("purchaseReturnId", header.getId(), "lineId", line.getId(), "serialNumberId", serialId, "sourcePurchaseReceiptId", receipt.getId())
                    );
                }
            } else if (!originalBatches.isEmpty()) {
                for (ErpReturnDtos.ReturnBatchSelection batchSelection : reqLine.batchSelections()) {
                    InventoryBatch batch = inventoryBatchRepository.findById(batchSelection.batchId())
                            .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchSelection.batchId()));
                    PurchaseReturnLineBatch link = new PurchaseReturnLineBatch();
                    link.setPurchaseReturnLineId(line.getId());
                    link.setBatchId(batch.getId());
                    link.setQuantity(batchSelection.quantity());
                    link.setBaseQuantity(batchSelection.baseQuantity());
                    purchaseReturnLineBatchRepository.save(link);

                    inventoryPostingService.postMovement(
                            organizationId, branchId, header.getWarehouseId(), line.getProductId(), batch.getId(),
                            line.getUomId(), batchSelection.quantity(), batchSelection.baseQuantity(), "OUT",
                            ErpInventoryMovementTypes.PURCHASE_RETURN, "purchase_return", header.getId(), header.getReturnNumber(),
                            line.getUnitCost(),
                            ErpJsonPayloads.of("purchaseReturnId", header.getId(), "lineId", line.getId(), "batchId", batch.getId(), "sourcePurchaseReceiptId", receipt.getId())
                    );
                }
            } else {
                inventoryPostingService.postMovement(
                        organizationId,
                        branchId,
                        header.getWarehouseId(),
                        line.getProductId(),
                        null,
                        line.getUomId(),
                        line.getQuantity(),
                        line.getBaseQuantity(),
                        "OUT",
                        ErpInventoryMovementTypes.PURCHASE_RETURN,
                        "purchase_return",
                        header.getId(),
                        header.getReturnNumber(),
                        line.getUnitCost(),
                        ErpJsonPayloads.of("purchaseReturnId", header.getId(), "lineId", line.getId(), "sourcePurchaseReceiptId", receipt.getId())
                );
            }

            subtotal = subtotal.add(line.getTaxableAmount());
            taxAmount = taxAmount.add(line.getCgstAmount()).add(line.getSgstAmount()).add(line.getIgstAmount()).add(line.getCessAmount());
            totalAmount = totalAmount.add(line.getLineAmount());
        }

        header.setSubtotal(subtotal);
        header.setTaxAmount(taxAmount);
        header.setTotalAmount(totalAmount);
        header = purchaseReturnRepository.save(header);
        accountingPostingService.postPurchaseReturn(header);

        auditEventWriter.write(
                organizationId, branchId, "PURCHASE_RETURN_POSTED", "purchase_return", header.getId(), header.getReturnNumber(),
                "POST", header.getWarehouseId(), null, header.getSupplierId(), "Purchase return posted",
                ErpJsonPayloads.of("returnId", header.getId(), "originalPurchaseReceiptId", header.getOriginalPurchaseReceiptId(), "total", header.getTotalAmount())
        );

        return toPurchaseReturnResponse(header, purchaseReturnLineRepository.findByPurchaseReturnIdOrderByIdAsc(header.getId()));
    }

    private BigDecimal proportional(BigDecimal amount, BigDecimal ratio) {
        return safe(amount).multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeDisposition(String disposition) {
        if (disposition == null || disposition.isBlank()) {
            return "RESTOCK";
        }
        return disposition.trim().toUpperCase();
    }

    private String normalizeInspectionStatus(String inspectionStatus) {
        if (inspectionStatus == null || inspectionStatus.isBlank()) {
            return "APPROVED";
        }
        return inspectionStatus.trim().toUpperCase();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateReturnTracking(
            List<Long> serialNumberIds,
            List<ErpReturnDtos.ReturnBatchSelection> batchSelections,
            List<?> originalSerials,
            List<?> originalBatches,
            BigDecimal baseQuantity,
            String label
    ) {
        boolean expectsSerials = originalSerials != null && !originalSerials.isEmpty();
        boolean expectsBatches = originalBatches != null && !originalBatches.isEmpty();
        boolean hasSerials = serialNumberIds != null && !serialNumberIds.isEmpty();
        boolean hasBatches = batchSelections != null && !batchSelections.isEmpty();

        if (expectsSerials) {
            if (!hasSerials || hasBatches) {
                throw new BusinessException("Tracked return requires exact serial numbers for " + label);
            }
            if (BigDecimal.valueOf(serialNumberIds.size()).compareTo(baseQuantity) != 0) {
                throw new BusinessException("Serial return count must equal base quantity for " + label);
            }
            return;
        }
        if (expectsBatches) {
            if (!hasBatches || hasSerials) {
                throw new BusinessException("Tracked return requires exact batch selection for " + label);
            }
            BigDecimal totalBatchBase = batchSelections.stream()
                    .map(ErpReturnDtos.ReturnBatchSelection::baseQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalBatchBase.compareTo(baseQuantity) != 0) {
                throw new BusinessException("Batch return base quantity mismatch for " + label);
            }
            return;
        }
        if (hasSerials || hasBatches) {
            throw new BusinessException("Standard return should not include serial or batch details for " + label);
        }
    }

    private ErpReturnResponses.SalesReturnResponse toSalesReturnResponse(SalesReturn header, List<SalesReturnLine> lines) {
        Map<Long, List<ErpReturnResponses.ReturnSerialDetailResponse>> salesSerials = lines.stream()
                .collect(java.util.stream.Collectors.toMap(
                        SalesReturnLine::getId,
                        line -> salesReturnLineSerialRepository.findBySalesReturnLineId(line.getId()).stream()
                                .map(item -> new ErpReturnResponses.ReturnSerialDetailResponse(item.getSerialNumberId()))
                                .toList()
                ));
        Map<Long, List<ErpReturnResponses.ReturnBatchDetailResponse>> salesBatches = lines.stream()
                .collect(java.util.stream.Collectors.toMap(
                        SalesReturnLine::getId,
                        line -> salesReturnLineBatchRepository.findBySalesReturnLineId(line.getId()).stream()
                                .map(item -> new ErpReturnResponses.ReturnBatchDetailResponse(item.getBatchId(), item.getQuantity(), item.getBaseQuantity()))
                                .toList()
                ));
        return new ErpReturnResponses.SalesReturnResponse(
                header.getId(), header.getOrganizationId(), header.getBranchId(), header.getWarehouseId(),
                header.getCustomerId(), header.getOriginalSalesInvoiceId(), header.getReturnNumber(), header.getReturnDate(),
                header.getSellerGstin(), header.getCustomerGstin(), header.getPlaceOfSupplyStateCode(), header.getReason(),
                header.getRemarks(), header.getInspectionNotes(), header.getSubtotal(), header.getTaxAmount(), header.getTotalAmount(), header.getStatus(),
                lines.stream().map(line -> new ErpReturnResponses.SalesReturnLineResponse(
                        line.getId(), line.getOriginalSalesInvoiceLineId(), line.getProductId(), line.getUomId(),
                        line.getQuantity(), line.getBaseQuantity(), line.getUnitPrice(), line.getDiscountAmount(),
                        line.getTaxableAmount(), line.getTaxRate(), line.getCgstAmount(), line.getSgstAmount(),
                        line.getIgstAmount(), line.getCessAmount(), line.getLineAmount(), line.getTotalCostAtReturn(),
                        line.getDisposition(), line.getReason(), line.getInspectionStatus(), line.getInspectionNotes(),
                        salesSerials.getOrDefault(line.getId(), List.of()),
                        salesBatches.getOrDefault(line.getId(), List.of())
                )).toList()
        );
    }

    private ErpReturnResponses.PurchaseReturnResponse toPurchaseReturnResponse(PurchaseReturn header, List<PurchaseReturnLine> lines) {
        Map<Long, List<ErpReturnResponses.ReturnSerialDetailResponse>> purchaseSerials = lines.stream()
                .collect(java.util.stream.Collectors.toMap(
                        PurchaseReturnLine::getId,
                        line -> purchaseReturnLineSerialRepository.findByPurchaseReturnLineId(line.getId()).stream()
                                .map(item -> new ErpReturnResponses.ReturnSerialDetailResponse(item.getSerialNumberId()))
                                .toList()
                ));
        Map<Long, List<ErpReturnResponses.ReturnBatchDetailResponse>> purchaseBatches = lines.stream()
                .collect(java.util.stream.Collectors.toMap(
                        PurchaseReturnLine::getId,
                        line -> purchaseReturnLineBatchRepository.findByPurchaseReturnLineId(line.getId()).stream()
                                .map(item -> new ErpReturnResponses.ReturnBatchDetailResponse(item.getBatchId(), item.getQuantity(), item.getBaseQuantity()))
                                .toList()
                ));
        return new ErpReturnResponses.PurchaseReturnResponse(
                header.getId(), header.getOrganizationId(), header.getBranchId(), header.getWarehouseId(),
                header.getSupplierId(), header.getOriginalPurchaseReceiptId(), header.getReturnNumber(), header.getReturnDate(),
                header.getSellerGstin(), header.getSupplierGstin(), header.getPlaceOfSupplyStateCode(), header.getReason(),
                header.getRemarks(), header.getSubtotal(), header.getTaxAmount(), header.getTotalAmount(), header.getStatus(),
                lines.stream().map(line -> new ErpReturnResponses.PurchaseReturnLineResponse(
                        line.getId(), line.getOriginalPurchaseReceiptLineId(), line.getProductId(), line.getUomId(),
                        line.getQuantity(), line.getBaseQuantity(), line.getUnitCost(), line.getTaxableAmount(),
                        line.getTaxRate(), line.getCgstAmount(), line.getSgstAmount(), line.getIgstAmount(),
                        line.getCessAmount(), line.getLineAmount(), line.getReason(),
                        purchaseSerials.getOrDefault(line.getId(), List.of()),
                        purchaseBatches.getOrDefault(line.getId(), List.of())
                )).toList()
        );
    }

    private void postApprovedSalesReturnSerials(SalesReturn header, SalesReturnLine line, SalesInvoiceLine original, SalesInvoice invoice) {
        List<Long> serialIds = salesReturnLineSerialRepository.findBySalesReturnLineId(line.getId()).stream()
                .map(SalesReturnLineSerial::getSerialNumberId)
                .toList();
        if (serialIds.isEmpty()) {
            throw new BusinessException("Sales return line " + line.getId() + " is missing serial selections");
        }
        for (Long serialId : serialIds) {
            SerialNumber serial = serialNumberRepository.findById(serialId)
                    .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialId));
            serial.setStatus(ErpDocumentStatuses.RETURNED);
            serial.setCurrentWarehouseId(header.getWarehouseId());
            serial.setCurrentCustomerId(null);
            serialNumberRepository.save(serial);

            inventoryPostingService.postMovement(
                    header.getOrganizationId(), header.getBranchId(), header.getWarehouseId(), line.getProductId(), serial.getBatchId(),
                    line.getUomId(), BigDecimal.ONE, BigDecimal.ONE, "IN",
                    ErpInventoryMovementTypes.SALES_RETURN, "sales_return", header.getId(), header.getReturnNumber(),
                    line.getUnitCostAtReturn(),
                    ErpJsonPayloads.of("salesReturnId", header.getId(), "lineId", line.getId(), "serialNumberId", serialId, "sourceInvoiceId", invoice.getId())
            );
        }
        for (ProductOwnership ownership : productOwnershipRepository.findBySalesInvoiceLineId(original.getId())) {
            if (serialIds.contains(ownership.getSerialNumberId())) {
                ownership.setStatus(ErpDocumentStatuses.RETURNED);
                productOwnershipRepository.save(ownership);
            }
        }
    }

    private void postApprovedSalesReturnBatches(SalesReturn header, SalesReturnLine line, SalesInvoice invoice) {
        List<SalesReturnLineBatch> batchLinks = salesReturnLineBatchRepository.findBySalesReturnLineId(line.getId());
        if (batchLinks.isEmpty()) {
            throw new BusinessException("Sales return line " + line.getId() + " is missing batch selections");
        }
        for (SalesReturnLineBatch link : batchLinks) {
            InventoryBatch batch = inventoryBatchRepository.findById(link.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + link.getBatchId()));
            inventoryPostingService.postMovement(
                    header.getOrganizationId(), header.getBranchId(), header.getWarehouseId(), line.getProductId(), batch.getId(),
                    line.getUomId(), link.getQuantity(), link.getBaseQuantity(), "IN",
                    ErpInventoryMovementTypes.SALES_RETURN, "sales_return", header.getId(), header.getReturnNumber(),
                    line.getUnitCostAtReturn(),
                    ErpJsonPayloads.of("salesReturnId", header.getId(), "lineId", line.getId(), "batchId", batch.getId(), "sourceInvoiceId", invoice.getId())
            );
        }
    }
}
