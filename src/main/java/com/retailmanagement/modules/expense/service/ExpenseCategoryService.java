package com.retailmanagement.modules.expense.service;

import com.retailmanagement.modules.expense.dto.request.ExpenseCategoryRequest;
import com.retailmanagement.modules.expense.dto.response.ExpenseCategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ExpenseCategoryService {

    ExpenseCategoryResponse createCategory(ExpenseCategoryRequest request);

    ExpenseCategoryResponse updateCategory(Long id, ExpenseCategoryRequest request);

    ExpenseCategoryResponse getCategoryById(Long id);

    ExpenseCategoryResponse getCategoryByCode(String categoryCode);

    ExpenseCategoryResponse getCategoryByName(String name);

    Page<ExpenseCategoryResponse> getAllCategories(Pageable pageable);

    List<ExpenseCategoryResponse> getRootCategories();

    List<ExpenseCategoryResponse> getSubCategories(Long parentId);

    List<ExpenseCategoryResponse> getCategoriesByType(String type);

    List<ExpenseCategoryResponse> searchCategories(String searchTerm);

    void deleteCategory(Long id);

    void activateCategory(Long id);

    void deactivateCategory(Long id);

    ExpenseCategoryResponse updateBudget(Long id, BigDecimal budgetAmount);

    BigDecimal getTotalExpensesForCategory(Long categoryId, String period);

    boolean isCategoryNameUnique(String name);

    boolean isCategoryCodeUnique(String categoryCode);
}