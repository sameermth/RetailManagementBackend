package com.retailmanagement.modules.inventory.dto.response;

import com.retailmanagement.modules.product.dto.response.ProductListResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long id;
    private ProductListResponse product;
    private WarehouseResponse warehouse;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer minimumStock;
    private Integer maximumStock;
    private Integer reorderPoint;
    private Integer reorderQuantity;
    private String binLocation;
    private String shelfNumber;
    private Double averageCost;
    private Double lastPurchasePrice;
    private String stockStatus;
    private LocalDateTime lastStockTakeDate;
    private LocalDateTime lastMovementDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}