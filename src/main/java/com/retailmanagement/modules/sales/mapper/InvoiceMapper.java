package com.retailmanagement.modules.sales.mapper;

import com.retailmanagement.modules.sales.dto.response.InvoiceResponse;
import com.retailmanagement.modules.sales.model.Invoice;
import com.retailmanagement.modules.sales.model.Sale;
import com.retailmanagement.modules.sales.model.SaleItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "sale", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Invoice toEntity(Sale sale);

    @Mapping(target = "saleId", source = "sale.id")
    @Mapping(target = "items", expression = "java(mapItems(invoice.getSale().getItems()))")
    InvoiceResponse toResponse(Invoice invoice);

    default List<InvoiceResponse.InvoiceItemDTO> mapItems(List<SaleItem> items) {
        return items.stream()
                .map(item -> InvoiceResponse.InvoiceItemDTO.builder()
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .discount(item.getDiscountAmount())
                        .tax(item.getTaxAmount())
                        .total(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());
    }
}