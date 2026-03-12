package com.retailmanagement.modules.customer.mapper;

import com.retailmanagement.modules.customer.dto.request.CustomerRequest;
import com.retailmanagement.modules.customer.dto.response.CustomerResponse;
import com.retailmanagement.modules.customer.dto.response.CustomerSummaryResponse;
import com.retailmanagement.modules.customer.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalDueAmount", ignore = true)
    @Mapping(target = "lastReminderSent", ignore = true)
    @Mapping(target = "lastDueDate", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "loyaltyTier", ignore = true)
    @Mapping(target = "totalPurchaseAmount", ignore = true)
    @Mapping(target = "lastPurchaseDate", ignore = true)
    @Mapping(target = "dues", ignore = true)
    @Mapping(target = "sales", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Customer toEntity(CustomerRequest request);

    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "totalPurchases", expression = "java(customer.getSales() != null ? customer.getSales().size() : 0)")
    @Mapping(target = "averagePurchaseValue", ignore = true)
    CustomerSummaryResponse toSummaryResponse(Customer customer);

    List<CustomerResponse> toResponseList(List<Customer> customers);
    List<CustomerSummaryResponse> toSummaryResponseList(List<Customer> customers);
}