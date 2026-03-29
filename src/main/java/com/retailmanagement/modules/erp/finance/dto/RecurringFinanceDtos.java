package com.retailmanagement.modules.erp.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RecurringFinanceDtos {
    private RecurringFinanceDtos() {}

    public record CreateRecurringJournalRequest(
            Long organizationId,
            Long branchId,
            @NotBlank String voucherType,
            @NotBlank String frequency,
            LocalDate startDate,
            LocalDate nextRunDate,
            LocalDate endDate,
            String remarks,
            Boolean isActive,
            @NotEmpty List<@Valid CreateRecurringJournalLineRequest> lines
    ) {}

    public record CreateRecurringJournalLineRequest(
            @NotNull Long accountId,
            @DecimalMin(value = "0.00") BigDecimal debitAmount,
            @DecimalMin(value = "0.00") BigDecimal creditAmount,
            String narrative,
            Long customerId,
            Long supplierId
    ) {}

    public record RunRecurringJournalRequest(
            LocalDate runDate
    ) {}
}
