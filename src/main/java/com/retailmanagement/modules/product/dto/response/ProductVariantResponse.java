package com.retailmanagement.modules.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private Long id;
    private String sku;
    private String size;
    private String color;
    private String style;
    private BigDecimal additionalPrice;
    private Integer stockQuantity;
    private Boolean isActive;
}