package com.retailmanagement.modules.purchase.mapper;

import com.retailmanagement.modules.purchase.dto.request.ReceiptItemRequest;
import com.retailmanagement.modules.purchase.dto.response.ReceiptItemResponse;
import com.retailmanagement.modules.purchase.model.PurchaseReceiptItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReceiptItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "receipt", ignore = true)
    @Mapping(target = "purchaseItem", ignore = true)
    @Mapping(target = "product", ignore = true)
    PurchaseReceiptItem toEntity(ReceiptItemRequest request);

    @Mapping(target = "purchaseItemId", source = "purchaseItem.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSku", source = "product.sku")
    ReceiptItemResponse toResponse(PurchaseReceiptItem item);

    List<ReceiptItemResponse> toResponseList(List<PurchaseReceiptItem> items);
}