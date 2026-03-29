package com.retailmanagement.modules.erp.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
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

    public record OrganizationSubscriptionResponse(
            Long organizationId,
            Long subscriptionVersion,
            String planCode,
            String planName,
            String status,
            LocalDate startsOn,
            LocalDate endsOn,
            Boolean autoRenew,
            Set<String> featureCodes
    ) {}
}
