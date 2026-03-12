package com.retailmanagement.modules.sales.mapper;

import com.retailmanagement.modules.sales.dto.request.SaleItemRequest;
import com.retailmanagement.modules.sales.dto.response.SaleItemResponse;
import com.retailmanagement.modules.sales.model.SaleItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SaleItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sale", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    SaleItem toEntity(SaleItemRequest request);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSku", source = "product.sku")
    SaleItemResponse toResponse(SaleItem saleItem);

    List<SaleItemResponse> toResponseList(List<SaleItem> saleItems);
}