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
            Long productOwnershipId,
            List<Long> productOwnershipIds,
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

    public record SalesDocumentLineResponse(
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
            BigDecimal lineAmount,
            String remarks
    ) {}

    public record SalesQuoteResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            String quoteType,
            String quoteNumber,
            LocalDate quoteDate,
            LocalDate validUntil,
            String sellerGstin,
            String customerGstin,
            String placeOfSupplyStateCode,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            Long convertedSalesOrderId,
            Long convertedSalesInvoiceId,
            String status,
            String remarks,
            List<SalesDocumentLineResponse> lines
    ) {}

    public record SalesQuoteSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            String quoteType,
            String quoteNumber,
            LocalDate quoteDate,
            LocalDate validUntil,
            BigDecimal totalAmount,
            Long convertedSalesOrderId,
            Long convertedSalesInvoiceId,
            String status
    ) {}

    public record SalesOrderResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            Long sourceQuoteId,
            String orderNumber,
            LocalDate orderDate,
            String sellerGstin,
            String customerGstin,
            String placeOfSupplyStateCode,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            Long convertedSalesInvoiceId,
            String status,
            String remarks,
            List<SalesDocumentLineResponse> lines
    ) {}

    public record SalesOrderSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            Long sourceQuoteId,
            String orderNumber,
            LocalDate orderDate,
            BigDecimal totalAmount,
            Long convertedSalesInvoiceId,
            String status
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
            List<SalesInvoiceLineResponse> lines,
            List<SalesInvoiceAllocationResponse> allocations
    ) {}

    public record SalesInvoiceAllocationResponse(
            Long customerReceiptId,
            String receiptNumber,
            LocalDate receiptDate,
            String paymentMethod,
            String referenceNumber,
            BigDecimal receiptAmount,
            BigDecimal allocatedAmount,
            String status
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
