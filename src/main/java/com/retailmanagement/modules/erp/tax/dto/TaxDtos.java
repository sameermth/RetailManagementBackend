package com.retailmanagement.modules.erp.tax.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    public record GstThresholdSettingsRequest(Boolean gstThresholdAlertEnabled) {}

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
            String effectiveRegistrationScope,
            boolean hasScopeConflict,
            List<String> scopeWarnings
    ) {}

    public record GstinLookupAddressResponse(
            String addressLine1,
            String addressLine2,
            String location,
            String district,
            String state,
            String stateCode,
            String postalCode,
            String country
    ) {}

    public record GstinLookupResponse(
            String gstin,
            boolean validFormat,
            boolean eligibleForNonGstFallback,
            String providerCode,
            String providerName,
            String lookupStatus,
            String legalName,
            String tradeName,
            String constitutionOfBusiness,
            String taxpayerType,
            String registrationStatus,
            Boolean active,
            LocalDate registrationDate,
            LocalDate cancellationDate,
            GstinLookupAddressResponse address,
            String message,
            LocalDateTime fetchedAt
    ) {}

    public record TaxComplianceDraftRequest(
            String transporterName,
            String transporterId,
            String transportMode,
            String vehicleNumber,
            BigDecimal distanceKm,
            String dispatchAddress,
            String shipToAddress,
            String notes
    ) {}

    public record TaxComplianceDocumentSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            String sourceType,
            Long sourceId,
            String documentType,
            String providerCode,
            String status,
            boolean eligibleForSubmission,
            String externalReference,
            String acknowledgementNumber,
            LocalDateTime acknowledgementDateTime,
            LocalDateTime generatedAt,
            LocalDateTime submittedAt,
            LocalDateTime lastSyncedAt,
            String errorMessage
    ) {}

    public record TaxComplianceDocumentResponse(
            Long id,
            Long organizationId,
            Long branchId,
            String sourceType,
            Long sourceId,
            String documentType,
            String providerCode,
            String providerName,
            String status,
            boolean eligibleForSubmission,
            List<String> warnings,
            String externalReference,
            String acknowledgementNumber,
            LocalDateTime acknowledgementDateTime,
            LocalDateTime generatedAt,
            LocalDateTime submittedAt,
            LocalDateTime lastSyncedAt,
            String errorMessage,
            Map<String, Object> payload,
            Map<String, Object> providerResponse
    ) {}
}
