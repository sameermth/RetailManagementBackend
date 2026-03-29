package com.retailmanagement.modules.erp.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RecurringSalesDtos {
    private RecurringSalesDtos() {}

    public record CreateRecurringSalesInvoiceRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long warehouseId,
            @NotNull Long customerId,
            Long priceListId,
            @NotBlank String frequency,
            LocalDate startDate,
            LocalDate nextRunDate,
            LocalDate endDate,
            @Min(0) Integer dueDays,
            String placeOfSupplyStateCode,
            String remarks,
            Boolean isActive,
            @NotEmpty List<@Valid CreateRecurringSalesInvoiceLineRequest> lines
    ) {}

    public record CreateRecurringSalesInvoiceLineRequest(
            @NotNull Long productId,
            @NotNull Long uomId,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal baseQuantity,
            @DecimalMin(value = "0.00") BigDecimal unitPrice,
            @DecimalMin(value = "0.00") BigDecimal discountAmount,
            Integer warrantyMonths,
            String remarks
    ) {}

    public record RunRecurringSalesInvoiceRequest(
            LocalDate runDate
    ) {}
}
