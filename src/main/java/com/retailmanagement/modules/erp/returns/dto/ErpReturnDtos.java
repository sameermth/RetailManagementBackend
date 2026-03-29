package com.retailmanagement.modules.erp.returns.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ErpReturnDtos {
    private ErpReturnDtos() {}

    public record CreateSalesReturnRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long originalSalesInvoiceId,
            LocalDate returnDate,
            String reason,
            String remarks,
            @NotEmpty List<@Valid CreateSalesReturnLineRequest> lines
    ) {}

    public record InspectSalesReturnRequest(
            String inspectionNotes,
            @NotEmpty List<@Valid InspectSalesReturnLineRequest> lines
    ) {}

    public record CreateSalesReturnLineRequest(
            @NotNull Long originalSalesInvoiceLineId,
            @NotNull @DecimalMin("0.000001") BigDecimal quantity,
            @NotNull @DecimalMin("0.000001") BigDecimal baseQuantity,
            List<Long> serialNumberIds,
            List<ReturnBatchSelection> batchSelections,
            String disposition,
            String reason
    ) {}

    public record CreatePurchaseReturnRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long originalPurchaseReceiptId,
            LocalDate returnDate,
            String reason,
            String remarks,
            @NotEmpty List<@Valid CreatePurchaseReturnLineRequest> lines
    ) {}

    public record CreatePurchaseReturnLineRequest(
            @NotNull Long originalPurchaseReceiptLineId,
            @NotNull @DecimalMin("0.000001") BigDecimal quantity,
            @NotNull @DecimalMin("0.000001") BigDecimal baseQuantity,
            List<Long> serialNumberIds,
            List<ReturnBatchSelection> batchSelections,
            String reason
    ) {}

    public record ReturnBatchSelection(
            @NotNull Long batchId,
            @NotNull @DecimalMin("0.000001") BigDecimal quantity,
            @NotNull @DecimalMin("0.000001") BigDecimal baseQuantity
    ) {}

    public record InspectSalesReturnLineRequest(
            @NotNull Long salesReturnLineId,
            String disposition,
            String inspectionStatus,
            String inspectionNotes
    ) {}
}
