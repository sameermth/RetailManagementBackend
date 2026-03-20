package com.retailmanagement.modules.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private CategoryResponse category;
    private BrandResponse brand;
    private BigDecimal unitPrice;
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
    private Boolean isActive;
    private Boolean isPerishable;
    private Integer shelfLifeDays;
    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;
    private Integer stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}