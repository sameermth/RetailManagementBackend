package com.retailmanagement.modules.purchase.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseReceiptRequest {

    @NotNull(message = "Purchase ID is required")
    private Long purchaseId;

    private LocalDateTime receiptDate;

    private List<ReceiptItemRequest> items;

    private String notes;
}