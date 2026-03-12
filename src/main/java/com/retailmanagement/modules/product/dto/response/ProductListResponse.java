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
public class ProductListResponse {
    private Long id;
    private String sku;
    private String name;
    private String categoryName;
    private String brandName;
    private BigDecimal unitPrice;
    private Integer stockQuantity;
    private String stockStatus;
    private Boolean isActive;
    private String primaryImageUrl;
}