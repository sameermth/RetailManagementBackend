package com.retailmanagement.modules.expense.mapper;

import com.retailmanagement.modules.expense.dto.request.RecurringExpenseRequest;
import com.retailmanagement.modules.expense.dto.response.RecurringExpenseResponse;
import com.retailmanagement.modules.expense.model.RecurringExpense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RecurringExpenseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recurringExpenseNumber", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "occurrencesGenerated", ignore = true)
    @Mapping(target = "nextGenerationDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    RecurringExpense toEntity(RecurringExpenseRequest request);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    RecurringExpenseResponse toResponse(RecurringExpense recurringExpense);

    List<RecurringExpenseResponse> toResponseList(List<RecurringExpense> recurringExpenses);
}