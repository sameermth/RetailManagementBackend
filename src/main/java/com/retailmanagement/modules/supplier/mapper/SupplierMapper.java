package com.retailmanagement.modules.supplier.mapper;

import com.retailmanagement.modules.supplier.dto.request.SupplierRequest;
import com.retailmanagement.modules.supplier.dto.response.SupplierResponse;
import com.retailmanagement.modules.supplier.dto.response.SupplierSummaryResponse;
import com.retailmanagement.modules.supplier.model.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {SupplierContactMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupplierMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplierCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "outstandingAmount", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    @Mapping(target = "purchases", ignore = true)
    @Mapping(target = "lastPurchaseDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Supplier toEntity(SupplierRequest request);

    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalPurchases", expression = "java(supplier.getPurchases() != null ? supplier.getPurchases().size() : 0)")
    SupplierResponse toResponse(Supplier supplier);

    SupplierSummaryResponse toSummaryResponse(Supplier supplier);

    List<SupplierResponse> toResponseList(List<Supplier> suppliers);
    List<SupplierSummaryResponse> toSummaryResponseList(List<Supplier> suppliers);
}