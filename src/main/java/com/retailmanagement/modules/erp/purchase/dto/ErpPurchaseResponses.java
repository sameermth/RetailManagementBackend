package com.retailmanagement.modules.erp.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ErpPurchaseResponses {
    private ErpPurchaseResponses() {}

    public record PurchaseLineResponse(
            Long id,
            Long productId,
            Long supplierProductId,
            Long productMasterId,
            String sku,
            String productName,
            String hsnCode,
            String supplierProductCode,
            Long uomId,
            BigDecimal quantity,
            BigDecimal baseQuantity,
            BigDecimal unitValue,
            BigDecimal taxableAmount,
            BigDecimal taxRate,
            BigDecimal cgstRate,
            BigDecimal cgstAmount,
            BigDecimal sgstRate,
            BigDecimal sgstAmount,
            BigDecimal igstRate,
            BigDecimal igstAmount,
            BigDecimal cessRate,
            BigDecimal cessAmount,
            BigDecimal lineAmount
    ) {}

    public record PurchaseOrderResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long supplierId,
            String poNumber,
            LocalDate poDate,
            String sellerGstin,
            String supplierGstin,
            String placeOfSupplyStateCode,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String status,
            List<PurchaseLineResponse> lines
    ) {}

    public record PurchaseOrderSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long supplierId,
            String poNumber,
            LocalDate poDate,
            String sellerGstin,
            String supplierGstin,
            String placeOfSupplyStateCode,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String status
    ) {}

    public record PurchaseReceiptResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long supplierId,
            String receiptNumber,
            LocalDate receiptDate,
            LocalDate dueDate,
            String sellerGstin,
            String supplierGstin,
            String placeOfSupplyStateCode,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            BigDecimal allocatedAmount,
            BigDecimal outstandingAmount,
            String status,
            List<PurchaseLineResponse> lines,
            List<PurchaseReceiptAllocationResponse> allocations
    ) {}

    public record PurchaseReceiptAllocationResponse(
            Long supplierPaymentId,
            String paymentNumber,
            LocalDate paymentDate,
            String paymentMethod,
            String referenceNumber,
            BigDecimal paymentAmount,
            BigDecimal allocatedAmount,
            String status
    ) {}

    public record PurchaseReceiptSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long supplierId,
            String receiptNumber,
            LocalDate receiptDate,
            LocalDate dueDate,
            String sellerGstin,
            String supplierGstin,
            String placeOfSupplyStateCode,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            BigDecimal allocatedAmount,
            BigDecimal outstandingAmount,
            String status
    ) {}

    public record SupplierPaymentResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long supplierId,
            String paymentNumber,
            LocalDate paymentDate,
            String paymentMethod,
            String referenceNumber,
            BigDecimal amount,
            String status,
            String remarks
    ) {}
}
