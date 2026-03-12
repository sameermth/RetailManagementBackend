package com.retailmanagement.modules.purchase.mapper;

import com.retailmanagement.modules.purchase.dto.request.SupplierPaymentRequest;
import com.retailmanagement.modules.purchase.dto.response.SupplierPaymentResponse;
import com.retailmanagement.modules.purchase.model.SupplierPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupplierPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentReference", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "purchase", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "receivedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    SupplierPayment toEntity(SupplierPaymentRequest request);

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "supplierCode", source = "supplier.supplierCode")
    @Mapping(target = "purchaseId", source = "purchase.id")
    @Mapping(target = "purchaseOrderNumber", source = "purchase.purchaseOrderNumber")
    SupplierPaymentResponse toResponse(SupplierPayment payment);

    List<SupplierPaymentResponse> toResponseList(List<SupplierPayment> payments);
}