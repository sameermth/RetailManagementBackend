package com.retailmanagement.modules.inventory.dto.response;

import com.retailmanagement.modules.inventory.enums.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {
    private Long id;
    private String referenceNumber;
    private String productName;
    private String productSku;
    private String fromWarehouse;
    private String toWarehouse;
    private MovementType movementType;
    private Integer quantity;
    private Integer previousStock;
    private Integer newStock;
    private Double unitCost;
    private Double totalCost;
    private String referenceType;
    private Long referenceId;
    private String reason;
    private String notes;
    private String performedBy;
    private LocalDateTime movementDate;
}