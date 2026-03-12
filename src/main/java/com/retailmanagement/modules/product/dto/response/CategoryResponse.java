package com.retailmanagement.modules.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private Long parentCategoryId;
    private String parentCategoryName;
    private List<CategoryResponse> subCategories;
    private String imageUrl;
    private Boolean isActive;
    private Integer displayOrder;
    private Integer productCount;
}