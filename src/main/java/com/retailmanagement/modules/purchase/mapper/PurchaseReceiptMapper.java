package com.retailmanagement.modules.purchase.mapper;

import com.retailmanagement.modules.purchase.dto.request.PurchaseReceiptRequest;
import com.retailmanagement.modules.purchase.dto.response.PurchaseReceiptResponse;
import com.retailmanagement.modules.purchase.model.PurchaseReceipt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {ReceiptItemMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseReceiptMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "receiptNumber", ignore = true)
    @Mapping(target = "purchase", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PurchaseReceipt toEntity(PurchaseReceiptRequest request);

    @Mapping(target = "purchaseId", source = "purchase.id")
    @Mapping(target = "purchaseOrderNumber", source = "purchase.purchaseOrderNumber")
    PurchaseReceiptResponse toResponse(PurchaseReceipt receipt);

    List<PurchaseReceiptResponse> toResponseList(List<PurchaseReceipt> receipts);
}