package com.retailmanagement.modules.erp.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class ErpExpenseDtos {
    private ErpExpenseDtos() {}

    public record CreateExpenseCategoryRequest(
            Long organizationId,
            @NotBlank String code,
            @NotBlank String name,
            Long expenseAccountId
    ) {}

    public record CreateExpenseRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long expenseCategoryId,
            LocalDate expenseDate,
            LocalDate dueDate,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String paymentMethod,
            String receiptUrl,
            String remarks,
            Boolean markPaid
    ) {}

    public record PayExpenseRequest(
            @NotBlank String paymentMethod,
            LocalDate paidDate,
            String remarks
    ) {}
}
