package com.retailmanagement.modules.erp.returns.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ErpReturnResponses {
    private ErpReturnResponses() {}

    public record ReturnSerialDetailResponse(
            Long serialNumberId
    ) {}

    public record ReturnBatchDetailResponse(
            Long batchId,
            BigDecimal quantity,
            BigDecimal baseQuantity
    ) {}

    public record SalesReturnSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            Long originalSalesInvoiceId,
            String returnNumber,
            LocalDate returnDate,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String status
    ) {}

    public record SalesReturnLineResponse(
            Long id,
            Long originalSalesInvoiceLineId,
            Long productId,
            Long uomId,
            BigDecimal quantity,
            BigDecimal baseQuantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxRate,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal cessAmount,
            BigDecimal lineAmount,
            BigDecimal totalCostAtReturn,
            String disposition,
            String reason,
            String inspectionStatus,
            String inspectionNotes,
            List<ReturnSerialDetailResponse> serials,
            List<ReturnBatchDetailResponse> batches
    ) {}

    public record SalesReturnResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            Long originalSalesInvoiceId,
            String returnNumber,
            LocalDate returnDate,
            String sellerGstin,
            String customerGstin,
            String placeOfSupplyStateCode,
            String reason,
            String remarks,
            String inspectionNotes,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String status,
            List<SalesReturnLineResponse> lines
    ) {}

    public record PurchaseReturnSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long supplierId,
            Long originalPurchaseReceiptId,
            String returnNumber,
            LocalDate returnDate,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String status
    ) {}

    public record PurchaseReturnLineResponse(
            Long id,
            Long originalPurchaseReceiptLineId,
            Long productId,
            Long uomId,
            BigDecimal quantity,
            BigDecimal baseQuantity,
            BigDecimal unitCost,
            BigDecimal taxableAmount,
            BigDecimal taxRate,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal cessAmount,
            BigDecimal lineAmount,
            String reason,
            List<ReturnSerialDetailResponse> serials,
            List<ReturnBatchDetailResponse> batches
    ) {}

    public record PurchaseReturnResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long supplierId,
            Long originalPurchaseReceiptId,
            String returnNumber,
            LocalDate returnDate,
            String sellerGstin,
            String supplierGstin,
            String placeOfSupplyStateCode,
            String reason,
            String remarks,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String status,
            List<PurchaseReturnLineResponse> lines
    ) {}
}
