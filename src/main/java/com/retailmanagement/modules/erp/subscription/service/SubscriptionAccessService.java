package com.retailmanagement.modules.erp.subscription.service;

import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.subscription.entity.AccountSubscription;
import com.retailmanagement.modules.erp.subscription.entity.SubscriptionPlanFeature;
import com.retailmanagement.modules.erp.subscription.repository.AccountSubscriptionRepository;
import com.retailmanagement.modules.erp.subscription.repository.SubscriptionPlanFeatureRepository;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionAccessService {

    private final OrganizationRepository organizationRepository;
    private final AccountSubscriptionRepository accountSubscriptionRepository;
    private final SubscriptionPlanFeatureRepository subscriptionPlanFeatureRepository;

    public SubscriptionSnapshot currentSnapshot(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        Long ownerAccountId = organization.getOwnerAccountId();
        long organizationsUsed = ownerAccountId == null ? 0L
                : organizationRepository.countByOwnerAccountIdAndIsActiveTrue(ownerAccountId);
        AccountSubscription subscription = ownerAccountId == null
                ? null
                : accountSubscriptionRepository.findCurrentSubscription(ownerAccountId, LocalDate.now()).orElse(null);

        if (subscription == null) {
            return new SubscriptionSnapshot(
                organizationId,
                ownerAccountId,
                organization.getSubscriptionVersion(),
                null,
                "NONE",
                null,
                false,
                organizationsUsed,
                false,
                Set.of()
            );
        }

        Set<String> featureCodes = subscriptionPlanFeatureRepository.findByPlanIdAndIsEnabledTrue(subscription.getPlan().getId())
                .stream()
                .map(SubscriptionPlanFeature::getFeature)
                .map(feature -> feature.getCode())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return new SubscriptionSnapshot(
                organizationId,
                ownerAccountId,
                organization.getSubscriptionVersion(),
                subscription.getPlan().getCode(),
                subscription.getStatus(),
                subscription.getPlan().getMaxOrganizations(),
                Boolean.TRUE.equals(subscription.getPlan().getUnlimitedOrganizations()),
                organizationsUsed,
                canCreateOrganization(
                        organizationsUsed,
                        subscription.getPlan().getMaxOrganizations(),
                        Boolean.TRUE.equals(subscription.getPlan().getUnlimitedOrganizations())
                ),
                featureCodes
        );
    }

    public boolean hasFeature(Long organizationId, String featureCode) {
        if (featureCode == null || featureCode.isBlank()) {
            return true;
        }
        SubscriptionSnapshot snapshot = currentSnapshot(organizationId);
        return snapshot.featureCodes().contains(featureCode.trim().toLowerCase());
    }

    public void assertFeature(Long organizationId, String featureCode) {
        if (!hasFeature(organizationId, featureCode)) {
            throw new AccessDeniedException("Subscription does not include feature: " + featureCode);
        }
    }

    public record SubscriptionSnapshot(
            Long organizationId,
            Long ownerAccountId,
            Long subscriptionVersion,
            String planCode,
            String status,
            Integer maxOrganizations,
            Boolean unlimitedOrganizations,
            Long organizationsUsed,
            Boolean canCreateOrganization,
            Set<String> featureCodes
    ) {}

    private boolean canCreateOrganization(Long used, Integer maxOrganizations, boolean unlimitedOrganizations) {
        if (unlimitedOrganizations) {
            return true;
        }
        if (maxOrganizations == null) {
            return false;
        }
        return used < maxOrganizations;
    }
}
