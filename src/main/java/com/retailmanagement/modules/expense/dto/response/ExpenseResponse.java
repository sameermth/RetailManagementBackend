package com.retailmanagement.modules.expense.dto.response;

import com.retailmanagement.modules.expense.enums.ExpenseStatus;
import com.retailmanagement.modules.expense.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private String expenseNumber;
    private Long categoryId;
    private String categoryName;
    private String categoryCode;
    private LocalDateTime expenseDate;
    private String description;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private ExpenseStatus status;
    private String vendor;
    private String vendorInvoiceNumber;
    private String referenceNumber;
    private Long userId;
    private String userName;
    private String paidTo;
    private String receiptUrl;
    private String notes;
    private Boolean isReimbursable;
    private Boolean isBillable;
    private Long customerId;
    private String customerName;
    private Long projectId;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String rejectionReason;
    private List<ExpenseAttachmentResponse> attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}