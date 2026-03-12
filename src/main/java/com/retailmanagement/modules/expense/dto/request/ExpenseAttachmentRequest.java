package com.retailmanagement.modules.expense.dto.request;

import lombok.Data;

@Data
public class ExpenseAttachmentRequest {

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String fileUrl;

    private Boolean isReceipt;
}