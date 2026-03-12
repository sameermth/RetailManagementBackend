package com.retailmanagement.modules.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StockTransferRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "From warehouse ID is required")
    private Long fromWarehouseId;

    @NotNull(message = "To warehouse ID is required")
    private Long toWarehouseId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private String reason;

    private String notes;
}