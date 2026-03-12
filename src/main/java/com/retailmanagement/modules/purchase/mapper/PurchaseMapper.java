package com.retailmanagement.modules.purchase.mapper;

import com.retailmanagement.modules.purchase.dto.request.PurchaseRequest;
import com.retailmanagement.modules.purchase.dto.response.PurchaseResponse;
import com.retailmanagement.modules.purchase.dto.response.PurchaseSummaryResponse;
import com.retailmanagement.modules.purchase.model.Purchase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {PurchaseItemMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "purchaseOrderNumber", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "receivedDate", ignore = true)
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
    Purchase toEntity(PurchaseRequest request);

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "supplierCode", source = "supplier.supplierCode")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.username")
    PurchaseResponse toResponse(Purchase purchase);

    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "itemCount", expression = "java(purchase.getItems().size())")
    PurchaseSummaryResponse toSummaryResponse(Purchase purchase);

    List<PurchaseResponse> toResponseList(List<Purchase> purchases);
    List<PurchaseSummaryResponse> toSummaryResponseList(List<Purchase> purchases);
}