package com.retailmanagement.modules.erp.subscription.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.subscription.dto.SubscriptionDtos;
import com.retailmanagement.modules.erp.subscription.entity.OrganizationSubscription;
import com.retailmanagement.modules.erp.subscription.entity.SubscriptionPlan;
import com.retailmanagement.modules.erp.subscription.repository.OrganizationSubscriptionRepository;
import com.retailmanagement.modules.erp.subscription.repository.SubscriptionPlanFeatureRepository;
import com.retailmanagement.modules.erp.subscription.repository.SubscriptionPlanRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionManagementService {

    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final SubscriptionPlanFeatureRepository subscriptionPlanFeatureRepository;
    private final SubscriptionAccessService subscriptionAccessService;
    private final ErpAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public SubscriptionDtos.OrganizationSubscriptionResponse currentSubscription(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        OrganizationSubscription subscription = organizationSubscriptionRepository.findCurrentSubscription(organizationId, LocalDate.now())
                .orElse(null);

        if (subscription == null) {
            return new SubscriptionDtos.OrganizationSubscriptionResponse(
                    organizationId,
                    organization.getSubscriptionVersion(),
                    null,
                    null,
                    "NONE",
                    null,
                    null,
                    false,
                    Set.of()
            );
        }

        Set<String> featureCodes = subscriptionPlanFeatureRepository.findByPlanIdAndIsEnabledTrue(subscription.getPlan().getId())
                .stream()
                .map(planFeature -> planFeature.getFeature().getCode())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        return new SubscriptionDtos.OrganizationSubscriptionResponse(
                organizationId,
                organization.getSubscriptionVersion(),
                subscription.getPlan().getCode(),
                subscription.getPlan().getName(),
                subscription.getStatus(),
                subscription.getStartsOn(),
                subscription.getEndsOn(),
                subscription.getAutoRenew(),
                featureCodes
        );
    }

    public SubscriptionDtos.OrganizationSubscriptionResponse activateSubscription(
            Long organizationId,
            SubscriptionDtos.ActivateOrganizationSubscriptionRequest request
    ) {
        accessGuard.assertOrganizationAccess(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        SubscriptionPlan plan = subscriptionPlanRepository.findByCode(request.planCode().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + request.planCode()));

        String nextStatus = normalizeStatus(request.status());
        LocalDate startsOn = request.startsOn() == null ? LocalDate.now() : request.startsOn();

        List<OrganizationSubscription> activeSubscriptions = organizationSubscriptionRepository.findActiveSubscriptions(organizationId, LocalDate.now());
        for (OrganizationSubscription current : activeSubscriptions) {
            current.setStatus("CANCELLED");
            current.setEndsOn(startsOn.minusDays(1));
        }

        OrganizationSubscription subscription = new OrganizationSubscription();
        subscription.setOrganizationId(organizationId);
        subscription.setPlan(plan);
        subscription.setStatus(nextStatus);
        subscription.setStartsOn(startsOn);
        subscription.setEndsOn(request.endsOn());
        subscription.setAutoRenew(Boolean.TRUE.equals(request.autoRenew()));
        subscription.setNotes(request.notes());
        organizationSubscriptionRepository.save(subscription);

        organization.setSubscriptionVersion((organization.getSubscriptionVersion() == null ? 0L : organization.getSubscriptionVersion()) + 1);
        organizationRepository.save(organization);

        return currentSubscription(organizationId);
    }

    public SubscriptionAccessService.SubscriptionSnapshot currentSnapshot(Long organizationId) {
        return subscriptionAccessService.currentSnapshot(organizationId);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("TRIALING", "ACTIVE", "PAST_DUE", "CANCELLED", "EXPIRED").contains(normalized)) {
            throw new BusinessException("Unsupported subscription status: " + status);
        }
        return normalized;
    }
}
