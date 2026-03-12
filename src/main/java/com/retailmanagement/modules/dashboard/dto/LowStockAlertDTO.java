package com.retailmanagement.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertDTO {
    private Long productId;
    private String productName;
    private String sku;
    private String category;
    private Integer currentStock;
    private Integer reorderLevel;
    private Integer recommendedOrder;
    private String status; // "LOW_STOCK", "OUT_OF_STOCK", "REORDER"
}