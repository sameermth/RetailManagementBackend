package com.retailmanagement.modules.erp.subscription.service;

import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.subscription.entity.OrganizationSubscription;
import com.retailmanagement.modules.erp.subscription.entity.SubscriptionPlanFeature;
import com.retailmanagement.modules.erp.subscription.repository.OrganizationSubscriptionRepository;
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
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final SubscriptionPlanFeatureRepository subscriptionPlanFeatureRepository;

    public SubscriptionSnapshot currentSnapshot(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        OrganizationSubscription subscription = organizationSubscriptionRepository
                .findCurrentSubscription(organizationId, LocalDate.now())
                .orElse(null);

        if (subscription == null) {
            return new SubscriptionSnapshot(
                    organizationId,
                    organization.getSubscriptionVersion(),
                    null,
                    "NONE",
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
                organization.getSubscriptionVersion(),
                subscription.getPlan().getCode(),
                subscription.getStatus(),
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
            Long subscriptionVersion,
            String planCode,
            String status,
            Set<String> featureCodes
    ) {}
}
