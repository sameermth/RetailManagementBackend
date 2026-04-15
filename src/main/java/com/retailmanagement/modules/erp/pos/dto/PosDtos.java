package com.retailmanagement.modules.erp.pos.dto;

import com.retailmanagement.modules.erp.sales.dto.ErpSalesDtos;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PosDtos {
    private PosDtos() {}

    public record OpenPosSessionRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long warehouseId,
            String terminalName,
            @NotNull @DecimalMin(value = "0.00") BigDecimal openingCashAmount,
            String openingNotes
    ) {}

    public record ClosePosSessionRequest(
            Long organizationId,
            Long branchId,
            @DecimalMin(value = "0.00") BigDecimal countedClosingCashAmount,
            String closingNotes
    ) {}

    public record PosSellableLotResponse(
            Long batchId,
            String batchNumber,
            String batchType,
            BigDecimal availableBaseQuantity,
            BigDecimal suggestedSalePrice,
            BigDecimal mrp,
            LocalDate expiryOn,
            boolean selectedByScan
    ) {}

    public record PosCatalogLookupResponse(
            String query,
            String matchedBy,
            Long storeProductId,
            Long productId,
            String sku,
            String name,
            String description,
            Long baseUomId,
            Long taxGroupId,
            String hsnCode,
            String inventoryTrackingMode,
            Boolean serialTrackingEnabled,
            Boolean batchTrackingEnabled,
            Boolean fractionalQuantityAllowed,
            Boolean serviceItem,
            Boolean active,
            BigDecimal unitPrice,
            BigDecimal mrp,
            BigDecimal onHandBaseQuantity,
            BigDecimal reservedBaseQuantity,
            BigDecimal availableBaseQuantity,
            boolean lotSelectionRecommended,
            String pricingSource,
            String pricingWarning,
            Long matchedSerialId,
            String matchedSerialNumber,
            Long matchedBatchId,
            String matchedBatchNumber,
            List<PosSellableLotResponse> lots
    ) {}

    public record PosCatalogSearchItemResponse(
            Long storeProductId,
            Long productId,
            String sku,
            String name,
            Long baseUomId,
            String inventoryTrackingMode,
            Boolean serialTrackingEnabled,
            Boolean batchTrackingEnabled,
            Boolean serviceItem,
            BigDecimal unitPrice,
            BigDecimal mrp,
            BigDecimal availableBaseQuantity,
            String pricingSource,
            boolean exactSkuMatch,
            boolean exactNameMatch
    ) {}

    public record PosCheckoutLineRequest(
            @NotNull Long storeProductId,
            @NotNull Long uomId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal baseQuantity,
            BigDecimal discountAmount,
            List<Long> serialNumberIds,
            List<ErpSalesDtos.BatchSelection> batchSelections,
            Integer warrantyMonths
    ) {}

    public record PosCheckoutPaymentRequest(
            @NotBlank String paymentMethod,
            @DecimalMin(value = "0.01") BigDecimal amount,
            String referenceNumber,
            String remarks,
            Boolean autoAllocate
    ) {}

    public record PosCheckoutRequest(
            Long customerId,
            Boolean useWalkInCustomer,
            LocalDate invoiceDate,
            String remarks,
            @NotEmpty List<@Valid PosCheckoutLineRequest> lines,
            @Valid PosCheckoutPaymentRequest payment
    ) {}

    public record PosInvoiceSummaryResponse(
            Long id,
            String invoiceNumber,
            LocalDate invoiceDate,
            Long customerId,
            BigDecimal totalAmount,
            String status
    ) {}

    public record PosReceiptSummaryResponse(
            Long id,
            String receiptNumber,
            LocalDate receiptDate,
            Long customerId,
            String paymentMethod,
            BigDecimal amount,
            String status
    ) {}

    public record PosSessionSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            String sessionNumber,
            String terminalName,
            String status,
            Long openedByUserId,
            String openedByUsername,
            LocalDateTime openedAt,
            LocalDateTime closedAt,
            BigDecimal openingCashAmount,
            BigDecimal expectedClosingCashAmount,
            BigDecimal countedClosingCashAmount,
            BigDecimal cashVarianceAmount,
            int invoiceCount,
            int receiptCount,
            BigDecimal grossSalesAmount,
            BigDecimal totalCollectedAmount,
            BigDecimal cashCollectedAmount,
            BigDecimal upiCollectedAmount,
            BigDecimal cardCollectedAmount,
            BigDecimal bankCollectedAmount,
            BigDecimal otherCollectedAmount
    ) {}

    public record PosSessionResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            String sessionNumber,
            String terminalName,
            String status,
            Long openedByUserId,
            String openedByUsername,
            LocalDateTime openedAt,
            String openingNotes,
            Long closedByUserId,
            String closedByUsername,
            LocalDateTime closedAt,
            String closingNotes,
            BigDecimal openingCashAmount,
            BigDecimal expectedClosingCashAmount,
            BigDecimal countedClosingCashAmount,
            BigDecimal cashVarianceAmount,
            int invoiceCount,
            int receiptCount,
            BigDecimal grossSalesAmount,
            BigDecimal totalCollectedAmount,
            BigDecimal cashCollectedAmount,
            BigDecimal upiCollectedAmount,
            BigDecimal cardCollectedAmount,
            BigDecimal bankCollectedAmount,
            BigDecimal otherCollectedAmount,
            List<PosInvoiceSummaryResponse> invoices,
            List<PosReceiptSummaryResponse> receipts
    ) {}

    public record PosCheckoutResponse(
            PosSessionResponse session,
            Long customerId,
            String customerCode,
            String customerName,
            ErpSalesResponses.SalesInvoiceResponse invoice,
            ErpSalesResponses.CustomerReceiptResponse receipt
    ) {}
}
