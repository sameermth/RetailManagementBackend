package com.retailmanagement.modules.erp.foundation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class OrganizationDtos {
    private OrganizationDtos() {}

    public record CreateOrganizationRequest(
            @NotBlank String name,
            @NotBlank String code,
            String legalName,
            String phone,
            String email,
            String gstin,
            Boolean gstThresholdAlertEnabled,
            Boolean isActive
    ) {}

    public record UpdateOrganizationRequest(
            String name,
            String code,
            String legalName,
            String phone,
            String email,
            String gstin,
            Boolean gstThresholdAlertEnabled,
            Boolean isActive
    ) {}

    public record OrganizationResponse(
            Long id,
            String name,
            String code,
            String legalName,
            String phone,
            String email,
            String gstin,
            Long ownerAccountId,
            Boolean gstThresholdAlertEnabled,
            Long subscriptionVersion,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
