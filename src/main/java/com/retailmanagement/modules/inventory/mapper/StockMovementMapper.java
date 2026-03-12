package com.retailmanagement.modules.inventory.mapper;

import com.retailmanagement.modules.inventory.dto.request.StockMovementRequest;
import com.retailmanagement.modules.inventory.dto.response.StockMovementResponse;
import com.retailmanagement.modules.inventory.model.StockMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockMovementMapper {

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "fromWarehouse", ignore = true)
    @Mapping(target = "toWarehouse", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "previousStock", ignore = true)
    @Mapping(target = "newStock", ignore = true)
    @Mapping(target = "totalCost", ignore = true)
    StockMovement toEntity(StockMovementRequest request);

    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "fromWarehouse", source = "fromWarehouse.name")
    @Mapping(target = "toWarehouse", source = "toWarehouse.name")
    StockMovementResponse toResponse(StockMovement stockMovement);

    List<StockMovementResponse> toResponseList(List<StockMovement> stockMovements);
}