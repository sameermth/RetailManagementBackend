package com.retailmanagement.modules.erp.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            BigDecimal mrp,
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
            BigDecimal mrp,
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
            LocalDate expectedFulfillmentBy,
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
            LocalDate expectedFulfillmentBy,
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
            SalesInvoiceDispatchSummaryResponse dispatchSummary,
            SalesInvoicePaymentRequestSummaryResponse paymentRequestSummary,
            List<SalesInvoiceLineResponse> lines,
            List<SalesInvoiceAllocationResponse> allocations,
            List<SalesInvoicePaymentRequestResponse> paymentRequests
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
            String status,
            SalesInvoiceDispatchSummaryResponse dispatchSummary,
            SalesInvoicePaymentRequestSummaryResponse paymentRequestSummary
    ) {}

    public record SalesInvoiceDispatchSummaryResponse(
            String status,
            int dispatchCount,
            BigDecimal totalDispatchedBaseQuantity,
            BigDecimal totalDeliveredBaseQuantity,
            BigDecimal pendingBaseQuantity,
            LocalDate lastDispatchDate
    ) {}

    public record SalesDispatchLineResponse(
            Long id,
            Long salesInvoiceLineId,
            Long productId,
            Long uomId,
            BigDecimal quantity,
            BigDecimal baseQuantity,
            BigDecimal pickedQuantity,
            BigDecimal pickedBaseQuantity,
            Long pickedBinLocationId,
            BigDecimal packedQuantity,
            BigDecimal packedBaseQuantity,
            String remarks
    ) {}

    public record SalesDispatchResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long salesInvoiceId,
            String invoiceNumber,
            Long warehouseId,
            Long customerId,
            String dispatchNumber,
            LocalDate dispatchDate,
            LocalDate expectedDeliveryDate,
            String status,
            String transporterName,
            String transporterId,
            String vehicleNumber,
            String trackingNumber,
            String deliveryAddress,
            String remarks,
            LocalDateTime pickedAt,
            LocalDateTime packedAt,
            LocalDateTime dispatchedAt,
            LocalDateTime deliveredAt,
            LocalDateTime cancelledAt,
            String cancelReason,
            List<SalesDispatchLineResponse> lines
    ) {}

    public record SalesDispatchSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long salesInvoiceId,
            String invoiceNumber,
            String dispatchNumber,
            LocalDate dispatchDate,
            LocalDate expectedDeliveryDate,
            String status,
            String transporterName,
            String trackingNumber,
            LocalDateTime dispatchedAt,
            LocalDateTime deliveredAt
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

    public record SalesInvoicePaymentRequestSummaryResponse(
            String status,
            int activeRequestCount,
            LocalDate lastRequestedOn,
            LocalDate lastExpiresOn,
            String latestPaymentLinkUrl,
            String latestProviderCode,
            String latestProviderStatus
    ) {}

    public record PaymentGatewayProviderResponse(
            String providerCode,
            String providerName,
            boolean configured,
            boolean simulated,
            boolean supportsStatusSync
    ) {}

    public record SalesInvoicePaymentRequestResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long salesInvoiceId,
            Long customerId,
            String invoiceNumber,
            String requestNumber,
            LocalDate requestDate,
            LocalDate dueDate,
            LocalDate expiresOn,
            BigDecimal requestedAmount,
            BigDecimal invoiceAllocatedAmount,
            BigDecimal invoiceOutstandingAmount,
            String providerCode,
            String providerName,
            String providerReference,
            String providerStatus,
            String channel,
            String paymentLinkToken,
            String paymentLinkUrl,
            String status,
            LocalDateTime providerCreatedAt,
            LocalDateTime providerLastSyncedAt,
            java.util.Map<String, Object> providerPayload,
            LocalDateTime lastSentAt,
            LocalDateTime cancelledAt,
            String cancelReason,
            String remarks
    ) {}
}
