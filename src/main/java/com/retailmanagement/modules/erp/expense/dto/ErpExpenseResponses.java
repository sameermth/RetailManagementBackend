package com.retailmanagement.modules.erp.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ErpExpenseResponses {
    private ErpExpenseResponses() {}

    public record ExpenseCategoryResponse(
            Long id,
            Long organizationId,
            String code,
            String name,
            Long expenseAccountId,
            Boolean isActive
    ) {}

    public record ExpenseResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long expenseCategoryId,
            String expenseNumber,
            LocalDate expenseDate,
            LocalDate dueDate,
            BigDecimal amount,
            BigDecimal outstandingAmount,
            String status,
            String paymentMethod,
            String receiptUrl,
            String remarks
    ) {}
}
