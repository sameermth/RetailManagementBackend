package com.retailmanagement.modules.purchase.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptItemResponse {
    private Long id;
    private Long purchaseItemId;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer quantityReceived;
    private String batchNumber;
    private LocalDateTime expiryDate;
    private String location;
}