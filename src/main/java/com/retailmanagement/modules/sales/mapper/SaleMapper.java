package com.retailmanagement.modules.sales.mapper;

import com.retailmanagement.modules.sales.dto.request.SaleRequest;
import com.retailmanagement.modules.sales.dto.response.SaleResponse;
import com.retailmanagement.modules.sales.dto.response.SaleSummaryResponse;
import com.retailmanagement.modules.sales.model.Sale;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {SaleItemMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SaleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "pendingAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Sale toEntity(SaleRequest request);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "customerPhone", source = "customer.phone")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.username")
    SaleResponse toResponse(Sale sale);

    List<SaleResponse> toResponseList(List<Sale> sales);

    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "itemCount", expression = "java(sale.getItems().size())")
    SaleSummaryResponse toSummaryResponse(Sale sale);

    List<SaleSummaryResponse> toSummaryResponseList(List<Sale> sales);
}