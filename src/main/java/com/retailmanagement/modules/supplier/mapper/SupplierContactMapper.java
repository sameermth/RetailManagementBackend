package com.retailmanagement.modules.supplier.mapper;

import com.retailmanagement.modules.supplier.dto.request.SupplierContactRequest;
import com.retailmanagement.modules.supplier.dto.response.SupplierContactResponse;
import com.retailmanagement.modules.supplier.model.SupplierContact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupplierContactMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    SupplierContact toEntity(SupplierContactRequest request);

    SupplierContactResponse toResponse(SupplierContact contact);

    List<SupplierContactResponse> toResponseList(List<SupplierContact> contacts);
}