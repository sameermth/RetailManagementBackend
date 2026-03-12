package com.retailmanagement.modules.purchase.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReceiptResponse {
    private Long id;
    private String receiptNumber;
    private Long purchaseId;
    private String purchaseOrderNumber;
    private LocalDateTime receiptDate;
    private String receivedBy;
    private List<ReceiptItemResponse> items;
    private String notes;
    private LocalDateTime createdAt;
}