package com.retailmanagement.modules.erp.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class SubscriptionDtos {
    private SubscriptionDtos() {}

    public record ActivateOrganizationSubscriptionRequest(
            @NotBlank String planCode,
            String status,
            LocalDate startsOn,
            LocalDate endsOn,
            Boolean autoRenew,
            String notes
    ) {}

    public record CancelOrganizationSubscriptionRequest(
            LocalDate endsOn,
            String notes
    ) {}

    public record SubscriptionFeatureResponse(
            String code,
            String name,
            String moduleCode,
            String description,
            Boolean enabled,
            Integer featureLimit
    ) {}

    public record SubscriptionPlanResponse(
            Long id,
            String code,
            String name,
            String description,
            String billingPeriod,
            Integer maxOrganizations,
            Boolean unlimitedOrganizations,
            Boolean active,
            Set<String> featureCodes,
            List<SubscriptionFeatureResponse> features
    ) {}

    public record AccountSubscriptionHistoryResponse(
            Long id,
            Long accountId,
            Long organizationId,
            String organizationCode,
            String organizationName,
            String planCode,
            String planName,
            String status,
            LocalDate startsOn,
            LocalDate endsOn,
            Boolean autoRenew,
            LocalDateTime purchasedAt,
            LocalDate graceUntil,
            String notes
    ) {}

    public record OrganizationSubscriptionResponse(
            Long organizationId,
            Long ownerAccountId,
            Long subscriptionVersion,
            String planCode,
            String planName,
            String status,
            LocalDate startsOn,
            LocalDate endsOn,
            Boolean autoRenew,
            Integer maxOrganizations,
            Boolean unlimitedOrganizations,
            Long organizationsUsed,
            Boolean canCreateOrganization,
            Set<String> featureCodes
    ) {}
}
