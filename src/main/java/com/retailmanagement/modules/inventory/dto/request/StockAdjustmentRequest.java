package com.retailmanagement.modules.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustmentRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "New quantity is required")
    private Integer newQuantity;

    private String reason;

    private String notes;
}