package com.retailmanagement.modules.inventory.mapper;

import com.retailmanagement.modules.inventory.dto.request.WarehouseRequest;
import com.retailmanagement.modules.inventory.dto.response.WarehouseResponse;
import com.retailmanagement.modules.inventory.model.Warehouse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WarehouseMapper {

    Warehouse toEntity(WarehouseRequest request);

    @Mapping(target = "productCount", expression = "java(warehouse.getInventories() != null ? warehouse.getInventories().size() : 0)")
    @Mapping(target = "currentOccupancy", ignore = true)
    WarehouseResponse toResponse(Warehouse warehouse);

    List<WarehouseResponse> toResponseList(List<Warehouse> warehouses);
}