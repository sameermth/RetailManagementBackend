package com.retailmanagement.modules.purchase.mapper;

import com.retailmanagement.modules.purchase.dto.request.PurchaseItemRequest;
import com.retailmanagement.modules.purchase.dto.response.PurchaseItemResponse;
import com.retailmanagement.modules.purchase.model.PurchaseItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "purchase", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "receivedQuantity", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    PurchaseItem toEntity(PurchaseItemRequest request);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "status", expression = "java(determineStatus(purchaseItem))")
    PurchaseItemResponse toResponse(PurchaseItem purchaseItem);

    List<PurchaseItemResponse> toResponseList(List<PurchaseItem> purchaseItems);

    default String determineStatus(PurchaseItem item) {
        if (item.getReceivedQuantity() == null || item.getReceivedQuantity() == 0) {
            return "PENDING";
        } else if (item.getReceivedQuantity() < item.getQuantity()) {
            return "PARTIAL";
        } else if (item.getReceivedQuantity().equals(item.getQuantity())) {
            return "RECEIVED";
        }
        return "PENDING";
    }
}