package com.retailmanagement.modules.erp.party.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CustomerDtos {
    private CustomerDtos() {}

    public record UpsertCustomerRequest(
            String customerCode,
            @NotBlank String fullName,
            String customerType,
            String legalName,
            String tradeName,
            String phone,
            String email,
            String gstin,
            Long linkedOrganizationId,
            String billingAddress,
            String shippingAddress,
            String state,
            String stateCode,
            String contactPersonName,
            String contactPersonPhone,
            String contactPersonEmail,
            BigDecimal creditLimit,
            Boolean isPlatformLinked,
            String notes,
            String status
    ) {}

    public record CustomerResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long linkedOrganizationId,
            String customerCode,
            String fullName,
            String customerType,
            String legalName,
            String tradeName,
            String phone,
            String email,
            String gstin,
            String billingAddress,
            String shippingAddress,
            String state,
            String stateCode,
            String contactPersonName,
            String contactPersonPhone,
            String contactPersonEmail,
            Boolean isPlatformLinked,
            BigDecimal creditLimit,
            String notes,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record UpsertStoreCustomerTermsRequest(
            String customerSegment,
            BigDecimal creditLimit,
            Integer creditDays,
            Boolean loyaltyEnabled,
            BigDecimal loyaltyPointsBalance,
            String priceTier,
            String discountPolicy,
            Boolean isPreferred,
            Boolean isActive,
            LocalDate contractStart,
            LocalDate contractEnd,
            String remarks
    ) {}

    public record StoreCustomerTermsResponse(
            Long id,
            Long organizationId,
            Long customerId,
            String customerSegment,
            BigDecimal creditLimit,
            Integer creditDays,
            Boolean loyaltyEnabled,
            BigDecimal loyaltyPointsBalance,
            String priceTier,
            String discountPolicy,
            Boolean isPreferred,
            Boolean isActive,
            LocalDate contractStart,
            LocalDate contractEnd,
            String remarks,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
