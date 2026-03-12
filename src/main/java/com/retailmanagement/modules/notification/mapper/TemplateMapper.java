package com.retailmanagement.modules.notification.mapper;

import com.retailmanagement.modules.notification.dto.request.TemplateRequest;
import com.retailmanagement.modules.notification.dto.response.TemplateResponse;
import com.retailmanagement.modules.notification.model.NotificationTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TemplateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NotificationTemplate toEntity(TemplateRequest request);

    TemplateResponse toResponse(NotificationTemplate template);

    List<TemplateResponse> toResponseList(List<NotificationTemplate> templates);
}