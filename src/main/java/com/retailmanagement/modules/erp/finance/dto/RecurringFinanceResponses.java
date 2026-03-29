package com.retailmanagement.modules.erp.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RecurringFinanceResponses {
    private RecurringFinanceResponses() {}

    public record RecurringJournalSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            String templateNumber,
            String voucherType,
            String frequency,
            LocalDate startDate,
            LocalDate nextRunDate,
            LocalDate endDate,
            Boolean isActive,
            Long lastVoucherId
    ) {}

    public record RecurringJournalLineResponse(
            Long id,
            Long accountId,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            String narrative,
            Long customerId,
            Long supplierId
    ) {}

    public record RecurringJournalResponse(
            Long id,
            Long organizationId,
            Long branchId,
            String templateNumber,
            String voucherType,
            String frequency,
            LocalDate startDate,
            LocalDate nextRunDate,
            LocalDate endDate,
            String remarks,
            Boolean isActive,
            LocalDateTime lastRunAt,
            Long lastVoucherId,
            List<RecurringJournalLineResponse> lines
    ) {}
}
