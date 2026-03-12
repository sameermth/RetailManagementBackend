package com.retailmanagement.modules.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    private Long parentCategoryId;

    private String imageUrl;

    private Boolean isActive;

    private Integer displayOrder;
}