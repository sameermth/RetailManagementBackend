package com.retailmanagement.modules.expense.mapper;

import com.retailmanagement.modules.expense.dto.request.ExpenseCategoryRequest;
import com.retailmanagement.modules.expense.dto.response.ExpenseCategoryResponse;
import com.retailmanagement.modules.expense.model.ExpenseCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExpenseCategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryCode", ignore = true)
    @Mapping(target = "parentCategory", ignore = true)
    @Mapping(target = "subCategories", ignore = true)
    @Mapping(target = "expenses", ignore = true)
    @Mapping(target = "allocatedAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ExpenseCategory toEntity(ExpenseCategoryRequest request);

    @Mapping(target = "parentCategoryId", source = "parentCategory.id")
    @Mapping(target = "parentCategoryName", source = "parentCategory.name")
    @Mapping(target = "totalExpenses", ignore = true)
    @Mapping(target = "remainingBudget", ignore = true)
    ExpenseCategoryResponse toResponse(ExpenseCategory category);

    List<ExpenseCategoryResponse> toResponseList(List<ExpenseCategory> categories);
}