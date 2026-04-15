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
            BigDecimal receivedBaseQuantity,
            Long putawayBinLocationId,
            BigDecimal putawayQuantity,
            BigDecimal putawayBaseQuantity,
            BigDecimal unitValue,
            BigDecimal suggestedSalePrice,
            BigDecimal mrp,
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

    public record PurchaseOrderSupplierAccessResponse(
            Long purchaseOrderId,
            String accessToken,
            String portalUrl,
            LocalDate expiresOn,
            boolean active
    ) {}

    public record SupplierDispatchLineResponse(
            Long id,
            Long purchaseOrderLineId,
            Long productId,
            String sku,
            String productName,
            BigDecimal orderedQuantity,
            BigDecimal orderedBaseQuantity,
            BigDecimal dispatchedQuantity,
            BigDecimal dispatchedBaseQuantity,
            LocalDate expectedRemainingDispatchOn,
            String remarks
    ) {}

    public record SupplierDispatchNoticeResponse(
            Long id,
            Long purchaseOrderId,
            Long supplierId,
            String dispatchNumber,
            LocalDate dispatchDate,
            LocalDate expectedDeliveryDate,
            String supplierReferenceNumber,
            String transporterName,
            String vehicleNumber,
            String trackingNumber,
            String status,
            String remarks,
            List<SupplierDispatchLineResponse> lines
    ) {}

    public record PurchaseOrderSupplierDispatchSummaryResponse(
            int dispatchCount,
            BigDecimal totalDispatchedBaseQuantity,
            BigDecimal pendingBaseQuantity,
            LocalDate lastExpectedDeliveryDate,
            LocalDate lastExpectedRemainingDispatchOn
    ) {}

    public record SupplierPortalPurchaseOrderLineResponse(
            Long purchaseOrderLineId,
            Long productId,
            String sku,
            String productName,
            String hsnCode,
            BigDecimal orderedQuantity,
            BigDecimal orderedBaseQuantity,
            BigDecimal receivedBaseQuantity,
            BigDecimal alreadyNotifiedBaseQuantity,
            BigDecimal remainingBaseQuantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal lineAmount
    ) {}

    public record SupplierPortalPurchaseOrderResponse(
            Long purchaseOrderId,
            String poNumber,
            LocalDate poDate,
            String status,
            Long supplierId,
            String supplierName,
            String supplierCode,
            LocalDate accessExpiresOn,
            PurchaseOrderSupplierDispatchSummaryResponse dispatchSummary,
            List<SupplierPortalPurchaseOrderLineResponse> lines,
            List<SupplierDispatchNoticeResponse> dispatchNotices
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
            List<PurchaseLineResponse> lines,
            PurchaseOrderSupplierDispatchSummaryResponse dispatchSummary,
            List<SupplierDispatchNoticeResponse> dispatchNotices
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
            String putawayStatus,
            LocalDate putawayCompletedOn,
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
            String status,
            String putawayStatus
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
