package com.retailmanagement.modules.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertResponse {
    private Long productId;
    private String productName;
    private String productSku;
    private String category;
    private String warehouse;
    private Integer currentStock;
    private Integer minimumStock;
    private Integer reorderPoint;
    private Integer recommendedOrder;
    private String alertType; // LOW_STOCK, OUT_OF_STOCK, OVER_STOCK, EXPIRING_SOON
    private String severity; // LOW, MEDIUM, HIGH
    private String message;
}