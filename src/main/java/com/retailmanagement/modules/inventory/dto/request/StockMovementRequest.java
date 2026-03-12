package com.retailmanagement.modules.inventory.dto.request;

import com.retailmanagement.modules.inventory.enums.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StockMovementRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long fromWarehouseId;

    private Long toWarehouseId;

    @NotNull(message = "Movement type is required")
    private MovementType movementType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private Double unitCost;

    private String referenceType;

    private Long referenceId;

    private String reason;

    private String notes;
}