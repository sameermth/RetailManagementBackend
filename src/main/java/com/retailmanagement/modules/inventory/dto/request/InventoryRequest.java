package com.retailmanagement.modules.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class InventoryRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @PositiveOrZero(message = "Quantity must be zero or positive")
    private Integer quantity;

    private Integer minimumStock;

    private Integer maximumStock;

    private Integer reorderPoint;

    private Integer reorderQuantity;

    private String binLocation;

    private String shelfNumber;

    private Double averageCost;

    private Double lastPurchasePrice;
}