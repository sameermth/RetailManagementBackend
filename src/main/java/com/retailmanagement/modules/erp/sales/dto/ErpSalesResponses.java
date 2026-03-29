package com.retailmanagement.modules.erp.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ErpSalesResponses {
    private ErpSalesResponses() {}

    public record SalesInvoiceLineResponse(
            Long id,
            Long productId,
            Long uomId,
            String hsnCode,
            BigDecimal quantity,
            BigDecimal baseQuantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
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

    public record SalesInvoiceResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            String invoiceNumber,
            LocalDate invoiceDate,
            LocalDate dueDate,
            String sellerGstin,
            String customerGstin,
            String placeOfSupplyStateCode,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            BigDecimal allocatedAmount,
            BigDecimal outstandingAmount,
            String status,
            List<SalesInvoiceLineResponse> lines
    ) {}

    public record SalesInvoiceSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            String invoiceNumber,
            LocalDate invoiceDate,
            LocalDate dueDate,
            String sellerGstin,
            String customerGstin,
            String placeOfSupplyStateCode,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            BigDecimal allocatedAmount,
            BigDecimal outstandingAmount,
            String status
    ) {}

    public record CustomerReceiptResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long customerId,
            String receiptNumber,
            LocalDate receiptDate,
            String paymentMethod,
            String referenceNumber,
            BigDecimal amount,
            String status,
            String remarks
    ) {}
}
