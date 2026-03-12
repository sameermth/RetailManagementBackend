package com.retailmanagement.modules.sales.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaleRequest {

    private Long customerId;

    @NotNull(message = "User ID is required")
    private Long userId;

    private LocalDateTime saleDate;

    @NotNull(message = "Sale items are required")
    private List<SaleItemRequest> items;

    @PositiveOrZero(message = "Discount amount must be zero or positive")
    private BigDecimal discountAmount;

    @PositiveOrZero(message = "Discount percentage must be zero or positive")
    private BigDecimal discountPercentage;

    @PositiveOrZero(message = "Shipping amount must be zero or positive")
    private BigDecimal shippingAmount;

    private String paymentMethod;

    private LocalDateTime dueDate;

    private String notes;

    private String billingAddress;

    private String shippingAddress;
}