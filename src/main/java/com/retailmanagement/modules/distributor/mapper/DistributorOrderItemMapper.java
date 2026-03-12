package com.retailmanagement.modules.distributor.mapper;

import com.retailmanagement.modules.distributor.dto.request.DistributorOrderItemRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorOrderItemResponse;
import com.retailmanagement.modules.distributor.model.DistributorOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DistributorOrderItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "shippedQuantity", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    DistributorOrderItem toEntity(DistributorOrderItemRequest request);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "status", expression = "java(determineStatus(item))")
    DistributorOrderItemResponse toResponse(DistributorOrderItem item);

    List<DistributorOrderItemResponse> toResponseList(List<DistributorOrderItem> items);

    default String determineStatus(DistributorOrderItem item) {
        if (item.getShippedQuantity() == null || item.getShippedQuantity() == 0) {
            return "PENDING";
        } else if (item.getShippedQuantity() < item.getQuantity()) {
            return "PARTIAL";
        } else if (item.getShippedQuantity().equals(item.getQuantity())) {
            return "SHIPPED";
        }
        return "PENDING";
    }
}