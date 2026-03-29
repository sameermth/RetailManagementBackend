package com.retailmanagement.modules.erp.sales.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.finance.service.ErpAccountingPostingService;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBatchRepository;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.inventory.service.InventoryPostingService;
import com.retailmanagement.modules.erp.inventory.service.InventoryReservationService;
import com.retailmanagement.modules.erp.sales.entity.ProductOwnership;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.entity.SalesLineBatch;
import com.retailmanagement.modules.erp.sales.entity.SalesLineSerial;
import com.retailmanagement.modules.erp.sales.repository.ProductOwnershipRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesLineBatchRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesLineSerialRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesInvoicePostingService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SalesInvoiceLineRepository salesInvoiceLineRepository;
    private final SalesLineSerialRepository salesLineSerialRepository;
    private final SalesLineBatchRepository salesLineBatchRepository;
    private final ProductOwnershipRepository productOwnershipRepository;
    private final StoreProductRepository productRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryPostingService inventoryPostingService;
    private final InventoryReservationService inventoryReservationService;
    private final ErpAccountingPostingService accountingPostingService;
    private final AuditEventWriter auditEventWriter;

    public SalesInvoice finalizeApprovedInvoice(Long salesInvoiceId) {
        SalesInvoice invoice = salesInvoiceRepository.findById(salesInvoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + salesInvoiceId));
        if (ErpDocumentStatuses.POSTED.equals(invoice.getStatus())) {
            return invoice;
        }

        List<SalesInvoiceLine> lines = salesInvoiceLineRepository.findBySalesInvoiceIdOrderByIdAsc(invoice.getId());
        int soldSerialCount = 0;
        inventoryReservationService.consumeSalesInvoiceReservations(invoice);

        for (SalesInvoiceLine line : lines) {
            StoreProduct product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.getProductId()));
            List<SalesLineSerial> serialLinks = salesLineSerialRepository.findBySalesInvoiceLineId(line.getId());
            List<SalesLineBatch> batchLinks = salesLineBatchRepository.findBySalesInvoiceLineId(line.getId());

            if (Boolean.TRUE.equals(product.getSerialTrackingEnabled())) {
                if (serialLinks.isEmpty()) {
                    throw new BusinessException("Serialized sale cannot be posted without serial selections for product " + product.getSku());
                }
                for (SalesLineSerial link : serialLinks) {
                    SerialNumber serial = serialNumberRepository.findById(link.getSerialNumberId())
                            .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + link.getSerialNumberId()));
                    if (!invoice.getOrganizationId().equals(serial.getOrganizationId())) {
                        throw new BusinessException("Serial " + link.getSerialNumberId() + " does not belong to organization " + invoice.getOrganizationId());
                    }
                    if (!line.getProductId().equals(serial.getProductId())) {
                        throw new BusinessException("Serial " + serial.getSerialNumber() + " does not belong to product " + line.getProductId());
                    }
                    if (!ErpDocumentStatuses.SOLD.equals(serial.getStatus())) {
                        if (!ErpDocumentStatuses.IN_STOCK.equals(serial.getStatus())
                                && !ErpDocumentStatuses.ALLOCATED.equals(serial.getStatus())) {
                            throw new BusinessException("Serial " + serial.getSerialNumber() + " is not available for sale");
                        }
                        if (serial.getCurrentWarehouseId() == null || !invoice.getWarehouseId().equals(serial.getCurrentWarehouseId())) {
                            throw new BusinessException("Serial " + serial.getSerialNumber() + " is not in warehouse " + invoice.getWarehouseId());
                        }
                        serial.setStatus(ErpDocumentStatuses.SOLD);
                        serial.setCurrentWarehouseId(null);
                        serial.setCurrentCustomerId(invoice.getCustomerId());
                        if (serial.getWarrantyStartDate() == null) {
                            serial.setWarrantyStartDate(invoice.getInvoiceDate());
                        }
                        Integer warrantyMonths = line.getWarrantyMonths();
                        if (warrantyMonths != null && warrantyMonths > 0) {
                            serial.setWarrantyEndDate(invoice.getInvoiceDate().plusMonths(warrantyMonths));
                        }
                        serialNumberRepository.save(serial);
                    }

                    if (productOwnershipRepository.findBySalesInvoiceLineId(line.getId()).stream().noneMatch(o -> link.getSerialNumberId().equals(o.getSerialNumberId()))) {
                        ProductOwnership ownership = new ProductOwnership();
                        ownership.setOrganizationId(invoice.getOrganizationId());
                        ownership.setCustomerId(invoice.getCustomerId());
                        ownership.setProductId(line.getProductId());
                        ownership.setSerialNumberId(link.getSerialNumberId());
                        ownership.setSalesInvoiceId(invoice.getId());
                        ownership.setSalesInvoiceLineId(line.getId());
                        ownership.setOwnershipStartDate(invoice.getInvoiceDate());
                        ownership.setWarrantyStartDate(serial.getWarrantyStartDate());
                        ownership.setWarrantyEndDate(serial.getWarrantyEndDate());
                        ownership.setStatus(ErpDocumentStatuses.ACTIVE);
                        productOwnershipRepository.save(ownership);
                    }
                    soldSerialCount++;
                }
                continue;
            }

            if (!batchLinks.isEmpty()) {
                BigDecimal batchBase = BigDecimal.ZERO;
                for (SalesLineBatch batchLink : batchLinks) {
                    InventoryBatch batch = inventoryBatchRepository.findById(batchLink.getBatchId())
                            .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchLink.getBatchId()));
                    if (!invoice.getOrganizationId().equals(batch.getOrganizationId())) {
                        throw new BusinessException("Batch " + batchLink.getBatchId() + " does not belong to organization " + invoice.getOrganizationId());
                    }
                    if (!line.getProductId().equals(batch.getProductId())) {
                        throw new BusinessException("Batch " + batchLink.getBatchId() + " does not belong to product " + line.getProductId());
                    }
                    inventoryPostingService.postMovement(
                            invoice.getOrganizationId(),
                            invoice.getBranchId(),
                            invoice.getWarehouseId(),
                            line.getProductId(),
                            batchLink.getBatchId(),
                            line.getUomId(),
                            batchLink.getQuantity(),
                            batchLink.getBaseQuantity(),
                            "OUT",
                            "SALES_INVOICE",
                            "sales_invoice",
                            invoice.getId(),
                            invoice.getInvoiceNumber(),
                            BigDecimal.ZERO,
                            payloadJson(invoice, line.getProductId(), line.getId(), "BATCH")
                    );
                    batchBase = batchBase.add(batchLink.getBaseQuantity());
                }
                if (batchBase.compareTo(line.getBaseQuantity()) != 0) {
                    throw new BusinessException("Batch base quantity mismatch for product " + line.getProductId());
                }
                continue;
            }

            inventoryPostingService.postMovement(
                    invoice.getOrganizationId(),
                    invoice.getBranchId(),
                    invoice.getWarehouseId(),
                    line.getProductId(),
                    null,
                    line.getUomId(),
                    line.getQuantity(),
                    line.getBaseQuantity(),
                    "OUT",
                    "SALES_INVOICE",
                    "sales_invoice",
                    invoice.getId(),
                    invoice.getInvoiceNumber(),
                    BigDecimal.ZERO,
                    payloadJson(invoice, line.getProductId(), line.getId(), "STANDARD")
            );
        }

        BigDecimal estimatedCost = lines.stream()
                .map(SalesInvoiceLine::getTotalCostAtSale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setStatus(ErpDocumentStatuses.POSTED);
        if (invoice.getPostedAt() == null) {
            invoice.setPostedAt(LocalDateTime.now());
        }
        invoice = salesInvoiceRepository.save(invoice);
        accountingPostingService.postSalesInvoice(invoice, estimatedCost);

        auditEventWriter.write(
                invoice.getOrganizationId(),
                invoice.getBranchId(),
                "SALES_INVOICE_POSTED",
                "sales_invoice",
                invoice.getId(),
                invoice.getInvoiceNumber(),
                "POST",
                invoice.getWarehouseId(),
                invoice.getCustomerId(),
                null,
                "Sales invoice posted",
                ErpJsonPayloads.of(
                        "invoiceNumber", invoice.getInvoiceNumber(),
                        "invoiceId", invoice.getId(),
                        "serialCount", soldSerialCount,
                        "total", invoice.getTotalAmount()
                )
        );
        return invoice;
    }

    private String payloadJson(SalesInvoice invoice, Long productId, Long lineId, String mode) {
        return ErpJsonPayloads.of(
                "invoiceId", invoice.getId(),
                "invoiceNumber", invoice.getInvoiceNumber(),
                "productId", productId,
                "lineId", lineId,
                "mode", mode
        );
    }
}
