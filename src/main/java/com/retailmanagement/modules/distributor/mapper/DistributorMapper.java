package com.retailmanagement.modules.distributor.mapper;

import com.retailmanagement.modules.distributor.dto.request.DistributorRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorResponse;
import com.retailmanagement.modules.distributor.dto.response.DistributorSummaryResponse;
import com.retailmanagement.modules.distributor.model.Distributor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DistributorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "distributorCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "outstandingAmount", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "lastOrderDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Distributor toEntity(DistributorRequest request);

    @Mapping(target = "totalOrders", expression = "java(distributor.getOrders() != null ? distributor.getOrders().size() : 0)")
    @Mapping(target = "totalOrderValue", ignore = true)
    DistributorResponse toResponse(Distributor distributor);

    DistributorSummaryResponse toSummaryResponse(Distributor distributor);

    List<DistributorResponse> toResponseList(List<Distributor> distributors);
    List<DistributorSummaryResponse> toSummaryResponseList(List<Distributor> distributors);
}