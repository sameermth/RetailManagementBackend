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
            @DecimalMin(value = "0.00") BigDecimal unitPrice,
            BigDecimal discountAmount,
            String remarks
    ) {}

    public record CreateSalesInvoiceLineRequest(
            @NotNull Long productId,
            @NotNull Long uomId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal baseQuantity,
            @DecimalMin(value = "0.00") BigDecimal unitPrice,
            String priceOverrideReason,
            @DecimalMin(value = "0.00") BigDecimal taxRate,
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

    public record AllocateReceiptRequest(
            @NotEmpty List<@Valid ReceiptAllocationLine> allocations
    ) {}

    public record ReceiptAllocationLine(
            @NotNull Long salesInvoiceId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal allocatedAmount
    ) {}
}
