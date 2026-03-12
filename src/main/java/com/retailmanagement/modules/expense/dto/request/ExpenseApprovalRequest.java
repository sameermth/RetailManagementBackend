package com.retailmanagement.modules.expense.dto.request;

import lombok.Data;

@Data
public class ExpenseApprovalRequest {

    private Boolean approved;

    private String rejectionReason;

    private String comments;
}