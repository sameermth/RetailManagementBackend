package com.retailmanagement.modules.erp.tax.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TaxDtos {
    private TaxDtos() {}

    public record UpsertTaxRegistrationRequest(
            Long branchId,
            @NotBlank String registrationName,
            String legalName,
            @NotBlank String gstin,
            @NotBlank String registrationStateCode,
            String registrationStateName,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Boolean isDefault,
            Boolean isActive
    ) {}

    public record TaxRegistrationResponse(
            Long id,
            Long organizationId,
            Long branchId,
            String registrationType,
            String registrationName,
            String legalName,
            String gstin,
            String registrationStateCode,
            String registrationStateName,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Boolean isDefault,
            Boolean isActive
    ) {}

    public record GstThresholdSettingsRequest(
            @NotNull @DecimalMin("0.00") BigDecimal gstThresholdAmount,
            Boolean gstThresholdAlertEnabled
    ) {}

    public record GstThresholdSettingsResponse(
            Long organizationId,
            BigDecimal gstThresholdAmount,
            Boolean gstThresholdAlertEnabled
    ) {}

    public record GstThresholdStatusResponse(
            Long organizationId,
            BigDecimal financialYearTurnover,
            BigDecimal gstThresholdAmount,
            BigDecimal utilizationRatio,
            String alertLevel,
            Boolean gstRegistered,
            Boolean thresholdReached,
            Boolean alertEnabled,
            String message
    ) {}

    public record TaxRegistrationListResponse(
            Long requestedBranchId,
            LocalDate effectiveDate,
            List<TaxRegistrationResponse> registrations,
            TaxRegistrationResponse applicableOrganizationRegistration,
            TaxRegistrationResponse applicableBranchRegistration,
            TaxRegistrationResponse effectiveRegistration,
            String effectiveRegistrationScope
    ) {}
}
