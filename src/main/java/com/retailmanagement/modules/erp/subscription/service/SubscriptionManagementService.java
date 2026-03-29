package com.retailmanagement.modules.erp.subscription.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.subscription.dto.SubscriptionDtos;
import com.retailmanagement.modules.erp.subscription.entity.AccountSubscription;
import com.retailmanagement.modules.erp.subscription.entity.SubscriptionPlan;
import com.retailmanagement.modules.erp.subscription.repository.AccountSubscriptionRepository;
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
    private final AccountSubscriptionRepository accountSubscriptionRepository;
    private final SubscriptionPlanFeatureRepository subscriptionPlanFeatureRepository;
    private final SubscriptionAccessService subscriptionAccessService;
    private final ErpAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public SubscriptionDtos.OrganizationSubscriptionResponse currentSubscription(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        Long ownerAccountId = organization.getOwnerAccountId();
        AccountSubscription subscription = ownerAccountId == null
                ? null
                : accountSubscriptionRepository.findCurrentSubscription(ownerAccountId, LocalDate.now()).orElse(null);
        long organizationsUsed = ownerAccountId == null ? 0L
                : organizationRepository.countByOwnerAccountIdAndIsActiveTrue(ownerAccountId);

        if (subscription == null) {
            return new SubscriptionDtos.OrganizationSubscriptionResponse(
                    organizationId,
                    ownerAccountId,
                    organization.getSubscriptionVersion(),
                    null,
                    null,
                    "NONE",
                    null,
                    null,
                    false,
                    null,
                    false,
                    organizationsUsed,
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
                ownerAccountId,
                organization.getSubscriptionVersion(),
                subscription.getPlan().getCode(),
                subscription.getPlan().getName(),
                subscription.getStatus(),
                subscription.getStartsOn(),
                subscription.getEndsOn(),
                subscription.getAutoRenew(),
                subscription.getPlan().getMaxOrganizations(),
                Boolean.TRUE.equals(subscription.getPlan().getUnlimitedOrganizations()),
                organizationsUsed,
                canCreateOrganization(organizationsUsed, subscription.getPlan().getMaxOrganizations(), Boolean.TRUE.equals(subscription.getPlan().getUnlimitedOrganizations())),
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
        if (organization.getOwnerAccountId() == null) {
            throw new BusinessException("Organization does not have an owner account assigned");
        }
        SubscriptionPlan plan = subscriptionPlanRepository.findByCode(request.planCode().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + request.planCode()));
        long organizationsUsed = organizationRepository.countByOwnerAccountIdAndIsActiveTrue(organization.getOwnerAccountId());
        if (!Boolean.TRUE.equals(plan.getUnlimitedOrganizations())
                && plan.getMaxOrganizations() != null
                && organizationsUsed > plan.getMaxOrganizations()) {
            throw new BusinessException("Selected plan allows only " + plan.getMaxOrganizations()
                    + " organizations, but owner already has " + organizationsUsed + " active organizations");
        }

        String nextStatus = normalizeStatus(request.status());
        LocalDate startsOn = request.startsOn() == null ? LocalDate.now() : request.startsOn();

        List<AccountSubscription> activeSubscriptions = accountSubscriptionRepository.findActiveSubscriptions(organization.getOwnerAccountId(), LocalDate.now());
        for (AccountSubscription current : activeSubscriptions) {
            current.setStatus("CANCELLED");
            current.setEndsOn(startsOn.minusDays(1));
        }

        AccountSubscription subscription = new AccountSubscription();
        subscription.setAccountId(organization.getOwnerAccountId());
        subscription.setPlan(plan);
        subscription.setStatus(nextStatus);
        subscription.setStartsOn(startsOn);
        subscription.setEndsOn(request.endsOn());
        subscription.setAutoRenew(Boolean.TRUE.equals(request.autoRenew()));
        subscription.setNotes(request.notes());
        accountSubscriptionRepository.save(subscription);

        organizationRepository.findByOwnerAccountId(organization.getOwnerAccountId()).forEach(org -> {
            org.setSubscriptionVersion((org.getSubscriptionVersion() == null ? 0L : org.getSubscriptionVersion()) + 1);
            organizationRepository.save(org);
        });

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

    private boolean canCreateOrganization(long organizationsUsed, Integer maxOrganizations, boolean unlimitedOrganizations) {
        if (unlimitedOrganizations) {
            return true;
        }
        if (maxOrganizations == null) {
            return false;
        }
        return organizationsUsed < maxOrganizations;
    }
}
