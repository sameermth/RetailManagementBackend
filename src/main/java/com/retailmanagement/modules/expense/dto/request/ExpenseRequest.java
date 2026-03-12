package com.retailmanagement.modules.expense.dto.request;

import com.retailmanagement.modules.expense.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExpenseRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Expense date is required")
    private LocalDateTime expenseDate;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    private String vendor;

    private String vendorInvoiceNumber;

    private String referenceNumber;

    private Long userId;

    private String paidTo;

    private String notes;

    private Boolean isReimbursable;

    private Boolean isBillable;

    private Long customerId;

    private Long projectId;

    private List<ExpenseAttachmentRequest> attachments;
}