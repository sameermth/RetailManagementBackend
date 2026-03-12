package com.retailmanagement.modules.purchase.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private Integer receivedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalPrice;
    private String notes;
    private String status; // PENDING, PARTIAL, RECEIVED
}