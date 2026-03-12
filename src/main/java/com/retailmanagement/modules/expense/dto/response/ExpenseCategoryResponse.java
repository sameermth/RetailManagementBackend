package com.retailmanagement.modules.expense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategoryResponse {
    private Long id;
    private String categoryCode;
    private String name;
    private String description;
    private String type;
    private Long parentCategoryId;
    private String parentCategoryName;
    private BigDecimal budgetAmount;
    private BigDecimal allocatedAmount;
    private BigDecimal totalExpenses;
    private BigDecimal remainingBudget;
    private Boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}