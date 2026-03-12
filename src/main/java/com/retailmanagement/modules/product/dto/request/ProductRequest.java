package com.retailmanagement.modules.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    private Long categoryId;

    private Long brandId;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;

    @PositiveOrZero(message = "Cost price must be zero or positive")
    private BigDecimal costPrice;

    private BigDecimal gstRate;

    private String hsnCode;

    private String unitOfMeasure;

    private Integer reorderLevel;

    private Integer reorderQuantity;

    private String specifications;

    private String barcode;

    private String manufacturer;

    private String countryOfOrigin;

    private Boolean isPerishable;

    private Integer shelfLifeDays;

    private List<ProductImageRequest> images;

    private List<ProductVariantRequest> variants;
}