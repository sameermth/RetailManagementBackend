package com.retailmanagement.modules.expense.mapper;

import com.retailmanagement.modules.expense.dto.request.ExpenseRequest;
import com.retailmanagement.modules.expense.dto.response.ExpenseResponse;
import com.retailmanagement.modules.expense.model.Expense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {ExpenseAttachmentMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExpenseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expenseNumber", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "receiptUrl", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Expense toEntity(ExpenseRequest request);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryCode", source = "category.categoryCode")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.username")
    @Mapping(target = "customerName", ignore = true)
    ExpenseResponse toResponse(Expense expense);

    List<ExpenseResponse> toResponseList(List<Expense> expenses);
}