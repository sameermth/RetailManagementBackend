package com.retailmanagement.modules.product.service;

import com.retailmanagement.modules.product.dto.request.CategoryRequest;
import com.retailmanagement.modules.product.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    CategoryResponse getCategoryById(Long id);

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getRootCategories();

    List<CategoryResponse> getSubCategories(Long parentId);

    void deleteCategory(Long id);

    boolean isCategoryNameUnique(String name);
}