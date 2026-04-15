package com.retailmanagement.modules.erp.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ErpSalesDtos {
    private ErpSalesDtos() {}

    public record CreateSalesInvoiceRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long warehouseId,
            @NotNull Long customerId,
            Long priceListId,
            LocalDate invoiceDate,
            LocalDate dueDate,
            String placeOfSupplyStateCode,
            String remarks,
            @NotEmpty List<@Valid CreateSalesInvoiceLineRequest> lines
    ) {}

    public record CreateSalesQuoteRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long warehouseId,
            @NotNull Long customerId,
            @NotBlank String quoteType,
            LocalDate quoteDate,
            LocalDate validUntil,
            String placeOfSupplyStateCode,
            String remarks,
            @NotEmpty List<@Valid CreateSalesDocumentLineRequest> lines
    ) {}

    public record CreateSalesOrderRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long warehouseId,
            @NotNull Long customerId,
            LocalDate orderDate,
            LocalDate expectedFulfillmentBy,
            String placeOfSupplyStateCode,
            String remarks,
            @NotEmpty List<@Valid CreateSalesDocumentLineRequest> lines
    ) {}

    public record ConvertSalesQuoteRequest(
            Long organizationId,
            Long branchId,
            LocalDate targetDate,
            String remarks,
            List<@Valid ConvertTrackedSalesLineRequest> trackedLines
    ) {}

    public record ConvertSalesOrderRequest(
            Long organizationId,
            Long branchId,
            LocalDate targetDate,
            String remarks,
            List<@Valid ConvertTrackedSalesLineRequest> trackedLines
    ) {}

    public record CreateSalesDispatchRequest(
            Long organizationId,
            Long branchId,
            LocalDate dispatchDate,
            LocalDate expectedDeliveryDate,
            String transporterName,
            String transporterId,
            String vehicleNumber,
            String trackingNumber,
            String deliveryAddress,
            String remarks,
            @NotEmpty List<@Valid CreateSalesDispatchLineRequest> lines
    ) {}

    public record CreateSalesDispatchLineRequest(
            @NotNull Long salesInvoiceLineId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal baseQuantity,
            String remarks
    ) {}

    public record UpdateSalesDispatchStatusRequest(
            Long organizationId,
            Long branchId,
            @NotBlank String status,
            String remarks
    ) {}

    public record PickSalesDispatchRequest(
            Long organizationId,
            Long branchId,
            @NotEmpty List<@Valid PickSalesDispatchLineRequest> lines
    ) {}

    public record PickSalesDispatchLineRequest(
            @NotNull Long salesDispatchLineId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal pickedQuantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal pickedBaseQuantity,
            Long pickedBinLocationId
    ) {}

    public record PackSalesDispatchRequest(
            Long organizationId,
            Long branchId,
            @NotEmpty List<@Valid PackSalesDispatchLineRequest> lines
    ) {}

    public record PackSalesDispatchLineRequest(
            @NotNull Long salesDispatchLineId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal packedQuantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal packedBaseQuantity
    ) {}

    public record CancelSalesDocumentRequest(
            Long organizationId,
            Long branchId,
            @NotBlank String reason
    ) {}

    public record ConvertTrackedSalesLineRequest(
            @NotNull Long productId,
            List<Long> serialNumberIds,
            List<@Valid BatchSelection> batchSelections
    ) {}

    public record CreateSalesDocumentLineRequest(
            @NotNull Long productId,
            @NotNull Long uomId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal baseQuantity,
            BigDecimal discountAmount,
            String remarks
    ) {}

    public record CreateSalesInvoiceLineRequest(
            @NotNull Long productId,
            @NotNull Long uomId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal baseQuantity,
            BigDecimal discountAmount,
            List<Long> serialNumberIds,
            List<@Valid BatchSelection> batchSelections,
            Integer warrantyMonths
    ) {}

    public record BatchSelection(
            @NotNull Long batchId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal baseQuantity
    ) {}

    public record CreateCustomerReceiptRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long customerId,
            LocalDate receiptDate,
            @NotBlank String paymentMethod,
            String referenceNumber,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String remarks
    ) {}

    public record CreateSalesInvoicePaymentRequestRequest(
            Long organizationId,
            Long branchId,
            LocalDate requestDate,
            LocalDate dueDate,
            LocalDate expiresOn,
            @DecimalMin(value = "0.01") BigDecimal requestedAmount,
            String providerCode,
            String channel,
            String remarks
    ) {}

    public record CancelSalesInvoicePaymentRequest(
            Long organizationId,
            Long branchId,
            @NotBlank String reason
    ) {}

    public record AllocateReceiptRequest(
            @NotEmpty List<@Valid ReceiptAllocationLine> allocations
    ) {}

    public record ReceiptAllocationLine(
            @NotNull Long salesInvoiceId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal allocatedAmount
    ) {}
}
