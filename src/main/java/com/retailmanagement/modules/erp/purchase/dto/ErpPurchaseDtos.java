package com.retailmanagement.modules.erp.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ErpPurchaseDtos {
    private ErpPurchaseDtos() {}

    public record CreatePurchaseOrderRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long supplierId,
            LocalDate poDate,
            String placeOfSupplyStateCode,
            String remarks,
            @NotEmpty List<@Valid CreatePurchaseOrderLineRequest> lines
    ) {}

    public record CreatePurchaseOrderLineRequest(
            @NotNull Long productId,
            Long supplierProductId,
            @NotNull Long uomId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal baseQuantity,
            @NotNull @DecimalMin(value = "0.00") BigDecimal unitPrice,
            @DecimalMin(value = "0.00") BigDecimal taxRate
    ) {}

    public record CreatePurchaseReceiptRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long warehouseId,
            Long purchaseOrderId,
            @NotNull Long supplierId,
            LocalDate receiptDate,
            LocalDate dueDate,
            String placeOfSupplyStateCode,
            String remarks,
            @NotEmpty List<@Valid CreatePurchaseReceiptLineRequest> lines
    ) {}

    public record CreatePurchaseReceiptLineRequest(
            Long purchaseOrderLineId,
            @NotNull Long productId,
            Long supplierProductId,
            @NotNull Long uomId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal baseQuantity,
            @NotNull @DecimalMin(value = "0.00") BigDecimal unitCost,
            @DecimalMin(value = "0.00") BigDecimal taxRate,
            List<String> serialNumbers,
            List<@Valid CreateBatchReceiptLine> batchEntries
    ) {}

    public record CreateBatchReceiptLine(
            String batchNumber,
            String manufacturerBatchNumber,
            LocalDate manufacturedOn,
            LocalDate expiryOn,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal baseQuantity
    ) {}

    public record CreateSupplierPaymentRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long supplierId,
            LocalDate paymentDate,
            @NotBlank String paymentMethod,
            String referenceNumber,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String remarks
    ) {}

    public record AllocateSupplierPaymentRequest(
            @NotEmpty List<@Valid SupplierPaymentAllocationLine> allocations
    ) {}

    public record SupplierPaymentAllocationLine(
            @NotNull Long purchaseReceiptId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal allocatedAmount
    ) {}
}
