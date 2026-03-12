package com.retailmanagement.modules.purchase.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseRequest {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "User ID is required")
    private Long userId;

    private LocalDateTime expectedDeliveryDate;

    @PositiveOrZero(message = "Discount amount must be zero or positive")
    private BigDecimal discountAmount;

    @PositiveOrZero(message = "Discount percentage must be zero or positive")
    private BigDecimal discountPercentage;

    @PositiveOrZero(message = "Shipping amount must be zero or positive")
    private BigDecimal shippingAmount;

    private String paymentTerms;

    private String shippingMethod;

    private String notes;

    private String termsAndConditions;

    @NotNull(message = "Purchase items are required")
    private List<PurchaseItemRequest> items;
}