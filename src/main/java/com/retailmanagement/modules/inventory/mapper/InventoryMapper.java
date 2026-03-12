package com.retailmanagement.modules.inventory.mapper;

import com.retailmanagement.modules.inventory.dto.request.InventoryRequest;
import com.retailmanagement.modules.inventory.dto.response.InventoryResponse;
import com.retailmanagement.modules.inventory.model.Inventory;
import com.retailmanagement.modules.product.mapper.ProductMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {ProductMapper.class, WarehouseMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryMapper {

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "warehouse", ignore = true)
    Inventory toEntity(InventoryRequest request);

    @Mapping(target = "stockStatus", expression = "java(getStockStatus(inventory))")
    InventoryResponse toResponse(Inventory inventory);

    List<InventoryResponse> toResponseList(List<Inventory> inventories);

    default String getStockStatus(Inventory inventory) {
        if (inventory.getQuantity() <= 0) {
            return "OUT_OF_STOCK";
        } else if (inventory.getQuantity() <= inventory.getMinimumStock()) {
            return "LOW_STOCK";
        } else if (inventory.getQuantity() >= inventory.getMaximumStock()) {
            return "OVER_STOCK";
        }
        return "IN_STOCK";
    }
}