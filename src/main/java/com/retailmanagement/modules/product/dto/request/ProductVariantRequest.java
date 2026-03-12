package com.retailmanagement.modules.product.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantRequest {
    private String sku;
    private String size;
    private String color;
    private String style;
    private BigDecimal additionalPrice;
    private Integer stockQuantity;
}