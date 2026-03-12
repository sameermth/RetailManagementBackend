package com.retailmanagement.modules.distributor.mapper;

import com.retailmanagement.modules.distributor.dto.request.DistributorOrderRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorOrderResponse;
import com.retailmanagement.modules.distributor.dto.response.DistributorOrderItemResponse;
import com.retailmanagement.modules.distributor.model.DistributorOrder;
import com.retailmanagement.modules.distributor.model.DistributorOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {DistributorOrderItemMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DistributorOrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "distributor", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deliveredDate", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "pendingAmount", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    DistributorOrder toEntity(DistributorOrderRequest request);

    @Mapping(target = "distributorId", source = "distributor.id")
    @Mapping(target = "distributorName", source = "distributor.name")
    @Mapping(target = "distributorCode", source = "distributor.distributorCode")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.username")
    DistributorOrderResponse toResponse(DistributorOrder order);

    List<DistributorOrderResponse> toResponseList(List<DistributorOrder> orders);
}