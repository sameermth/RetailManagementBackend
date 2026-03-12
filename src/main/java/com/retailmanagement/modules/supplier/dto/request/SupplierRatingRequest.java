package com.retailmanagement.modules.supplier.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SupplierRatingRequest {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @Min(value = 1, message = "Quality rating must be between 1 and 5")
    @Max(value = 5, message = "Quality rating must be between 1 and 5")
    private Integer qualityRating;

    @Min(value = 1, message = "Delivery rating must be between 1 and 5")
    @Max(value = 5, message = "Delivery rating must be between 1 and 5")
    private Integer deliveryRating;

    @Min(value = 1, message = "Price rating must be between 1 and 5")
    @Max(value = 5, message = "Price rating must be between 1 and 5")
    private Integer priceRating;

    @Min(value = 1, message = "Communication rating must be between 1 and 5")
    @Max(value = 5, message = "Communication rating must be between 1 and 5")
    private Integer communicationRating;

    private String comments;

    private Long purchaseId;
}