package com.retailmanagement.modules.erp.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class BankReconciliationDtos {
    private BankReconciliationDtos() {}

    public record ImportBankStatementRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long accountId,
            @NotEmpty List<@Valid BankStatementLineRequest> lines
    ) {}

    public record BankStatementLineRequest(
            @NotNull LocalDate entryDate,
            LocalDate valueDate,
            String referenceNumber,
            String description,
            @DecimalMin(value = "0.00") BigDecimal debitAmount,
            @DecimalMin(value = "0.00") BigDecimal creditAmount,
            String remarks
    ) {}

    public record BankReconciliationQuery(
            @NotNull Long accountId,
            @NotNull LocalDate fromDate,
            @NotNull LocalDate toDate
    ) {}

    public record ReconcileBankStatementRequest(
            @NotNull Long ledgerEntryId,
            String remarks
    ) {}
}
