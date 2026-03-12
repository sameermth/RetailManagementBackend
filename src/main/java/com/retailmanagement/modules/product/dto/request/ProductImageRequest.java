package com.retailmanagement.modules.product.dto.request;

import lombok.Data;

@Data
public class ProductImageRequest {
    private String imageUrl;
    private Boolean isPrimary;
    private Integer displayOrder;
    private String altText;
}