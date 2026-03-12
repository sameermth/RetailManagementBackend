package com.retailmanagement.modules.sales.mapper;

import com.retailmanagement.modules.sales.dto.request.PaymentRequest;
import com.retailmanagement.modules.sales.dto.response.PaymentResponse;
import com.retailmanagement.modules.sales.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentReference", ignore = true)
    @Mapping(target = "sale", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "receivedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Payment toEntity(PaymentRequest request);

    @Mapping(target = "saleId", source = "sale.id")
    @Mapping(target = "invoiceNumber", source = "sale.invoiceNumber")
    PaymentResponse toResponse(Payment payment);

    List<PaymentResponse> toResponseList(List<Payment> payments);
}