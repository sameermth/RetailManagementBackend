package com.retailmanagement.modules.erp.returns.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.constants.ErpInventoryMovementTypes;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.finance.service.ErpAccountingPostingService;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBatchRepository;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.inventory.service.InventoryPostingService;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceipt;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceiptLine;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseReceiptLineRepository;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseReceiptRepository;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturn;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLine;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLineBatch;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLineSerial;
import com.retailmanagement.modules.erp.returns.entity.SalesReturn;
import com.retailmanagement.modules.erp.returns.entity.SalesReturnLine;
import com.retailmanagement.modules.erp.returns.entity.SalesReturnLineBatch;
import com.retailmanagement.modules.erp.returns.entity.SalesReturnLineSerial;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnLineBatchRepository;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnLineRepository;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnLineSerialRepository;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnLineBatchRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnLineRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnLineSerialRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnRepository;
import com.retailmanagement.modules.erp.sales.entity.ProductOwnership;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.repository.ProductOwnershipRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnPostingService {

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
    private final ProductOwnershipRepository productOwnershipRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final PurchaseReceiptLineRepository purchaseReceiptLineRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryPostingService inventoryPostingService;
    private final ErpAccountingPostingService accountingPostingService;
    private final AuditEventWriter auditEventWriter;

    public SalesReturn finalizeApprovedSalesReturn(Long salesReturnId) {
        SalesReturn header = salesReturnRepository.findById(salesReturnId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales return not found: " + salesReturnId));
        if (ErpDocumentStatuses.POSTED.equals(header.getStatus())) {
            return header;
        }
        Long originalSalesInvoiceId = header.getOriginalSalesInvoiceId();
        List<SalesReturnLine> lines = salesReturnLineRepository.findBySalesReturnIdOrderByIdAsc(salesReturnId);
        SalesInvoice invoice = salesInvoiceRepository.findById(originalSalesInvoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + originalSalesInvoiceId));

        BigDecimal inventoryValue = BigDecimal.ZERO;
        for (SalesReturnLine line : lines) {
            if (!"APPROVED".equals(line.getInspectionStatus())) {
                continue;
            }
            SalesInvoiceLine original = salesInvoiceLineRepository.findById(line.getOriginalSalesInvoiceLineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice line not found: " + line.getOriginalSalesInvoiceLineId()));

            List<SalesReturnLineSerial> serialLinks = salesReturnLineSerialRepository.findBySalesReturnLineId(line.getId());
            List<SalesReturnLineBatch> batchLinks = salesReturnLineBatchRepository.findBySalesReturnLineId(line.getId());

            if (!serialLinks.isEmpty()) {
                postApprovedSalesReturnSerials(header, line, original, invoice, serialLinks);
                if ("RESTOCK".equals(line.getDisposition())) {
                    inventoryValue = inventoryValue.add(line.getTotalCostAtReturn());
                }
            } else if ("RESTOCK".equals(line.getDisposition())) {
                if (!batchLinks.isEmpty()) {
                    postApprovedSalesReturnBatches(header, line, invoice, batchLinks);
                } else {
                    inventoryPostingService.postMovement(
                            header.getOrganizationId(),
                            header.getBranchId(),
                            header.getWarehouseId(),
                            null,
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
        header.setPostedAt(LocalDateTime.now());
        header = salesReturnRepository.save(header);
        accountingPostingService.postSalesReturn(header, inventoryValue);

        auditEventWriter.write(
                header.getOrganizationId(), header.getBranchId(), "SALES_RETURN_POSTED", "sales_return", header.getId(), header.getReturnNumber(),
                "POST", header.getWarehouseId(), header.getCustomerId(), null, "Sales return posted",
                ErpJsonPayloads.of("returnId", header.getId(), "originalSalesInvoiceId", header.getOriginalSalesInvoiceId(), "total", header.getTotalAmount())
        );
        return header;
    }

    public PurchaseReturn finalizeApprovedPurchaseReturn(Long purchaseReturnId) {
        PurchaseReturn header = purchaseReturnRepository.findById(purchaseReturnId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase return not found: " + purchaseReturnId));
        if (ErpDocumentStatuses.POSTED.equals(header.getStatus())) {
            return header;
        }
        Long originalPurchaseReceiptId = header.getOriginalPurchaseReceiptId();
        PurchaseReceipt receipt = purchaseReceiptRepository.findById(originalPurchaseReceiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase receipt not found: " + originalPurchaseReceiptId));
        List<PurchaseReturnLine> lines = purchaseReturnLineRepository.findByPurchaseReturnIdOrderByIdAsc(purchaseReturnId);

        for (PurchaseReturnLine line : lines) {
            PurchaseReceiptLine original = purchaseReceiptLineRepository.findById(line.getOriginalPurchaseReceiptLineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase receipt line not found: " + line.getOriginalPurchaseReceiptLineId()));
            List<PurchaseReturnLineSerial> serialLinks = purchaseReturnLineSerialRepository.findByPurchaseReturnLineId(line.getId());
            List<PurchaseReturnLineBatch> batchLinks = purchaseReturnLineBatchRepository.findByPurchaseReturnLineId(line.getId());

            if (!serialLinks.isEmpty()) {
                for (PurchaseReturnLineSerial link : serialLinks) {
                    SerialNumber serial = serialNumberRepository.findById(link.getSerialNumberId())
                            .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + link.getSerialNumberId()));
                    serial.setStatus(ErpDocumentStatuses.RETURNED);
                    serial.setCurrentWarehouseId(null);
                    serialNumberRepository.save(serial);

                    inventoryPostingService.postMovement(
                            header.getOrganizationId(), header.getBranchId(), header.getWarehouseId(), null, line.getProductId(), serial.getBatchId(),
                            line.getUomId(), BigDecimal.ONE, BigDecimal.ONE, "OUT",
                            ErpInventoryMovementTypes.PURCHASE_RETURN, "purchase_return", header.getId(), header.getReturnNumber(),
                            line.getUnitCost(),
                            ErpJsonPayloads.of("purchaseReturnId", header.getId(), "lineId", line.getId(), "serialNumberId", serial.getId(), "sourcePurchaseReceiptId", receipt.getId(), "sourcePurchaseReceiptLineId", original.getId())
                    );
                }
            } else if (!batchLinks.isEmpty()) {
                for (PurchaseReturnLineBatch link : batchLinks) {
                    InventoryBatch batch = inventoryBatchRepository.findById(link.getBatchId())
                            .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + link.getBatchId()));
                    inventoryPostingService.postMovement(
                            header.getOrganizationId(), header.getBranchId(), header.getWarehouseId(), null, line.getProductId(), batch.getId(),
                            line.getUomId(), link.getQuantity(), link.getBaseQuantity(), "OUT",
                            ErpInventoryMovementTypes.PURCHASE_RETURN, "purchase_return", header.getId(), header.getReturnNumber(),
                            line.getUnitCost(),
                            ErpJsonPayloads.of("purchaseReturnId", header.getId(), "lineId", line.getId(), "batchId", batch.getId(), "sourcePurchaseReceiptId", receipt.getId(), "sourcePurchaseReceiptLineId", original.getId())
                    );
                }
            } else {
                inventoryPostingService.postMovement(
                        header.getOrganizationId(),
                        header.getBranchId(),
                        header.getWarehouseId(),
                        null,
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
                        ErpJsonPayloads.of("purchaseReturnId", header.getId(), "lineId", line.getId(), "sourcePurchaseReceiptId", receipt.getId(), "sourcePurchaseReceiptLineId", original.getId())
                );
            }
        }

        header.setStatus(ErpDocumentStatuses.POSTED);
        header.setPostedAt(LocalDateTime.now());
        header = purchaseReturnRepository.save(header);
        accountingPostingService.postPurchaseReturn(header);

        auditEventWriter.write(
                header.getOrganizationId(), header.getBranchId(), "PURCHASE_RETURN_POSTED", "purchase_return", header.getId(), header.getReturnNumber(),
                "POST", header.getWarehouseId(), null, header.getSupplierId(), "Purchase return posted",
                ErpJsonPayloads.of("returnId", header.getId(), "originalPurchaseReceiptId", header.getOriginalPurchaseReceiptId(), "total", header.getTotalAmount())
        );
        return header;
    }

    private void postApprovedSalesReturnSerials(SalesReturn header, SalesReturnLine line, SalesInvoiceLine original,
                                                SalesInvoice invoice, List<SalesReturnLineSerial> serialLinks) {
        boolean restock = "RESTOCK".equals(line.getDisposition());
        List<Long> serialIds = serialLinks.stream().map(SalesReturnLineSerial::getSerialNumberId).toList();
        if (serialIds.isEmpty()) {
            throw new BusinessException("Sales return line " + line.getId() + " is missing serial selections");
        }
        for (Long serialId : serialIds) {
            SerialNumber serial = serialNumberRepository.findById(serialId)
                    .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialId));
            serial.setStatus(restock ? ErpDocumentStatuses.IN_STOCK : ErpDocumentStatuses.RETURNED);
            serial.setCurrentWarehouseId(header.getWarehouseId());
            serial.setCurrentCustomerId(null);
            serialNumberRepository.save(serial);

            if (restock) {
                inventoryPostingService.postMovement(
                        header.getOrganizationId(), header.getBranchId(), header.getWarehouseId(), null, line.getProductId(), serial.getBatchId(),
                        line.getUomId(), BigDecimal.ONE, BigDecimal.ONE, "IN",
                        ErpInventoryMovementTypes.SALES_RETURN, "sales_return", header.getId(), header.getReturnNumber(),
                        line.getUnitCostAtReturn(),
                        ErpJsonPayloads.of("salesReturnId", header.getId(), "lineId", line.getId(), "serialNumberId", serialId, "sourceInvoiceId", invoice.getId())
                );
            }
        }
        for (ProductOwnership ownership : productOwnershipRepository.findBySalesInvoiceLineId(original.getId())) {
            if (serialIds.contains(ownership.getSerialNumberId())) {
                ownership.setStatus(ErpDocumentStatuses.RETURNED);
                productOwnershipRepository.save(ownership);
            }
        }
    }

    private void postApprovedSalesReturnBatches(SalesReturn header, SalesReturnLine line, SalesInvoice invoice,
                                                List<SalesReturnLineBatch> batchLinks) {
        if (batchLinks.isEmpty()) {
            throw new BusinessException("Sales return line " + line.getId() + " is missing batch selections");
        }
        for (SalesReturnLineBatch link : batchLinks) {
            InventoryBatch batch = inventoryBatchRepository.findById(link.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + link.getBatchId()));
            inventoryPostingService.postMovement(
                    header.getOrganizationId(), header.getBranchId(), header.getWarehouseId(), null, line.getProductId(), batch.getId(),
                    line.getUomId(), link.getQuantity(), link.getBaseQuantity(), "IN",
                    ErpInventoryMovementTypes.SALES_RETURN, "sales_return", header.getId(), header.getReturnNumber(),
                    line.getUnitCostAtReturn(),
                    ErpJsonPayloads.of("salesReturnId", header.getId(), "lineId", line.getId(), "batchId", batch.getId(), "sourceInvoiceId", invoice.getId())
            );
        }
    }
}
