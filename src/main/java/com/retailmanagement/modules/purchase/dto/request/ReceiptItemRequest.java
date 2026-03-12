package com.retailmanagement.modules.purchase.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReceiptItemRequest {

    @NotNull(message = "Purchase item ID is required")
    private Long purchaseItemId;

    @NotNull(message = "Quantity received is required")
    @Positive(message = "Quantity received must be positive")
    private Integer quantityReceived;

    private String batchNumber;

    private LocalDateTime expiryDate;

    private String location;
}