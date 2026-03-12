package com.retailmanagement.modules.distributor.mapper;

import com.retailmanagement.modules.distributor.dto.request.DistributorPaymentRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorPaymentResponse;
import com.retailmanagement.modules.distributor.model.DistributorPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DistributorPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentReference", ignore = true)
    @Mapping(target = "distributor", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "receivedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    DistributorPayment toEntity(DistributorPaymentRequest request);

    @Mapping(target = "distributorId", source = "distributor.id")
    @Mapping(target = "distributorName", source = "distributor.name")
    @Mapping(target = "distributorCode", source = "distributor.distributorCode")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    DistributorPaymentResponse toResponse(DistributorPayment payment);

    List<DistributorPaymentResponse> toResponseList(List<DistributorPayment> payments);
}