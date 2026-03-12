package com.retailmanagement.modules.expense.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseCategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    private String type;

    private Long parentCategoryId;

    private BigDecimal budgetAmount;

    private Boolean isActive;

    private String notes;
}