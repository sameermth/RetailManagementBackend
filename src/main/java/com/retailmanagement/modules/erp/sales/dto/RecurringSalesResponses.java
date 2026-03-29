package com.retailmanagement.modules.erp.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RecurringSalesResponses {
    private RecurringSalesResponses() {}

    public record RecurringSalesInvoiceSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            String templateNumber,
            String frequency,
            LocalDate startDate,
            LocalDate nextRunDate,
            LocalDate endDate,
            Boolean isActive,
            Long lastSalesInvoiceId
    ) {}

    public record RecurringSalesInvoiceLineResponse(
            Long id,
            Long productId,
            Long uomId,
            BigDecimal quantity,
            BigDecimal baseQuantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            Integer warrantyMonths,
            String remarks
    ) {}

    public record RecurringSalesInvoiceResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            Long priceListId,
            String templateNumber,
            String frequency,
            LocalDate startDate,
            LocalDate nextRunDate,
            LocalDate endDate,
            Integer dueDays,
            String placeOfSupplyStateCode,
            String remarks,
            Boolean isActive,
            LocalDateTime lastRunAt,
            Long lastSalesInvoiceId,
            List<RecurringSalesInvoiceLineResponse> lines
    ) {}
}
