package com.retailmanagement.modules.expense.mapper;

import com.retailmanagement.modules.expense.dto.request.ExpenseAttachmentRequest;
import com.retailmanagement.modules.expense.dto.response.ExpenseAttachmentResponse;
import com.retailmanagement.modules.expense.model.ExpenseAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExpenseAttachmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expense", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    ExpenseAttachment toEntity(ExpenseAttachmentRequest request);

    ExpenseAttachmentResponse toResponse(ExpenseAttachment attachment);

    List<ExpenseAttachmentResponse> toResponseList(List<ExpenseAttachment> attachments);
}