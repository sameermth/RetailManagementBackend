package com.retailmanagement.modules.expense.controller;

import com.retailmanagement.modules.expense.dto.request.ExpenseCategoryRequest;
import com.retailmanagement.modules.expense.dto.response.ExpenseCategoryResponse;
import com.retailmanagement.modules.expense.service.ExpenseCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/expense-categories")
@RequiredArgsConstructor
@Tag(name = "Expense Categories", description = "Expense category management endpoints")
public class ExpenseCategoryController {

    private final ExpenseCategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Create a new expense category")
    public ResponseEntity<ExpenseCategoryResponse> createCategory(@Valid @RequestBody ExpenseCategoryRequest request) {
        return new ResponseEntity<>(categoryService.createCategory(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Update an existing expense category")
    public ResponseEntity<ExpenseCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense category by ID")
    public ResponseEntity<ExpenseCategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/code/{categoryCode}")
    @Operation(summary = "Get expense category by code")
    public ResponseEntity<ExpenseCategoryResponse> getCategoryByCode(@PathVariable String categoryCode) {
        return ResponseEntity.ok(categoryService.getCategoryByCode(categoryCode));
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get expense category by name")
    public ResponseEntity<ExpenseCategoryResponse> getCategoryByName(@PathVariable String name) {
        return ResponseEntity.ok(categoryService.getCategoryByName(name));
    }

    @GetMapping
    @Operation(summary = "Get all expense categories with pagination")
    public ResponseEntity<Page<ExpenseCategoryResponse>> getAllCategories(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(categoryService.getAllCategories(pageable));
    }

    @GetMapping("/root")
    @Operation(summary = "Get root categories")
    public ResponseEntity<List<ExpenseCategoryResponse>> getRootCategories() {
        return ResponseEntity.ok(categoryService.getRootCategories());
    }

    @GetMapping("/{parentId}/subcategories")
    @Operation(summary = "Get subcategories")
    public ResponseEntity<List<ExpenseCategoryResponse>> getSubCategories(@PathVariable Long parentId) {
        return ResponseEntity.ok(categoryService.getSubCategories(parentId));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get categories by type")
    public ResponseEntity<List<ExpenseCategoryResponse>> getCategoriesByType(@PathVariable String type) {
        return ResponseEntity.ok(categoryService.getCategoriesByType(type));
    }

    @GetMapping("/search")
    @Operation(summary = "Search categories")
    public ResponseEntity<List<ExpenseCategoryResponse>> searchCategories(@RequestParam String q) {
        return ResponseEntity.ok(categoryService.searchCategories(q));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an expense category")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Activate a category")
    public ResponseEntity<Void> activateCategory(@PathVariable Long id) {
        categoryService.activateCategory(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Deactivate a category")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/budget")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Update category budget")
    public ResponseEntity<ExpenseCategoryResponse> updateBudget(
            @PathVariable Long id,
            @RequestParam BigDecimal budgetAmount) {
        return ResponseEntity.ok(categoryService.updateBudget(id, budgetAmount));
    }

    @GetMapping("/{id}/total-expenses")
    @Operation(summary = "Get total expenses for category")
    public ResponseEntity<BigDecimal> getTotalExpensesForCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "MONTH") String period) {
        return ResponseEntity.ok(categoryService.getTotalExpensesForCategory(id, period));
    }

    @GetMapping("/check-name")
    @Operation(summary = "Check if category name is unique")
    public ResponseEntity<Boolean> checkCategoryNameUnique(@RequestParam String name) {
        return ResponseEntity.ok(categoryService.isCategoryNameUnique(name));
    }

    @GetMapping("/check-code")
    @Operation(summary = "Check if category code is unique")
    public ResponseEntity<Boolean> checkCategoryCodeUnique(@RequestParam String categoryCode) {
        return ResponseEntity.ok(categoryService.isCategoryCodeUnique(categoryCode));
    }
}