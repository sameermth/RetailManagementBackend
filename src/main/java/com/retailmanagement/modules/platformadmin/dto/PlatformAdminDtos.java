package com.retailmanagement.modules.platformadmin.dto;

import com.retailmanagement.modules.erp.foundation.dto.BranchDtos;
import com.retailmanagement.modules.erp.subscription.dto.SubscriptionDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PlatformAdminDtos {
    private PlatformAdminDtos() {}

    public record OverviewResponse(
            long totalStores,
            long activeStores,
            long totalOwnerAccounts,
            long totalUsers,
            long activeUsers,
            long openSupportItems,
            long feedbackItems,
            long activeReportSchedules,
            List<SubscriptionCountByPlan> subscriptionsByPlan,
            List<CountByLabel> usersByRole
    ) {}

    public record StoreSummaryResponse(
            Long organizationId,
            String organizationCode,
            String organizationName,
            Long ownerAccountId,
            Boolean active,
            int branchCount,
            int warehouseCount,
            int teamCount,
            String currentPlanCode,
            String currentPlanName,
            String currentSubscriptionStatus,
            Long subscriptionVersion,
            LocalDate subscriptionStartsOn,
            LocalDate subscriptionEndsOn
    ) {}

    public record SubscriptionSummaryResponse(
            Long organizationId,
            String organizationCode,
            String organizationName,
            Long ownerAccountId,
            String planCode,
            String planName,
            String status,
            LocalDate startsOn,
            LocalDate endsOn,
            Boolean autoRenew,
            Integer maxOrganizations,
            Boolean unlimitedOrganizations,
            Long organizationsUsed
    ) {}

    public record OwnerAccountReferenceResponse(
            Long accountId,
            String loginIdentifier,
            String fullName,
            String email,
            String phone,
            Boolean active
    ) {}

    public record TeamMemberSummaryResponse(
            Long organizationId,
            String organizationCode,
            String organizationName,
            Long userId,
            Long accountId,
            Long personId,
            String username,
            String fullName,
            String email,
            String phone,
            String roleCode,
            String roleName,
            String employeeCode,
            Long defaultBranchId,
            Boolean active,
            LocalDateTime createdAt
    ) {}

    public record SupportItemSummaryResponse(
            String itemType,
            Long organizationId,
            String organizationCode,
            String organizationName,
            Long branchId,
            Long referenceId,
            String referenceNumber,
            String status,
            String priorityOrType,
            String summary,
            LocalDate eventDate
    ) {}

    public record FeedbackSummaryResponse(
            Long organizationId,
            String organizationCode,
            String organizationName,
            Long serviceVisitId,
            Long serviceTicketId,
            String visitStatus,
            LocalDateTime scheduledAt,
            LocalDateTime completedAt,
            String customerFeedback
    ) {}

    public record PlatformReportsResponse(
            List<CountByLabel> storesByStatus,
            List<SubscriptionCountByPlan> subscriptionsByPlan,
            List<CountByLabel> usersByRole,
            List<CountByLabel> supportItemsByStatus,
            List<CountByLabel> feedbackByVisitStatus,
            long activeReportSchedules
    ) {}

    public record NotificationSummaryResponse(
            Long id,
            String notificationId,
            String type,
            String channel,
            String status,
            String priority,
            String title,
            String recipient,
            String referenceType,
            Long referenceId,
            LocalDateTime scheduledFor,
            LocalDateTime sentAt,
            LocalDateTime createdAt,
            Integer retryCount,
            String errorMessage
    ) {}

    public record CatalogProductGovernanceResponse(
            Long productId,
            String name,
            String brandName,
            String categoryName,
            String hsnCode,
            Boolean serviceItem,
            Boolean active,
            String governanceStatus,
            String qualityReviewStatus,
            Boolean blockNewStoreAdoption,
            Boolean blockTransactions,
            String governanceReason,
            LocalDateTime governanceUpdatedAt,
            long incidentCount
    ) {}

    public record UpdateProductGovernanceRequest(
            @NotBlank String governanceStatus,
            @NotBlank String qualityReviewStatus,
            @NotNull Boolean blockNewStoreAdoption,
            @NotNull Boolean blockTransactions,
            String governanceReason
    ) {}

    public record CatalogProductImpactStoreResponse(
            Long storeProductId,
            Long organizationId,
            String organizationCode,
            String organizationName,
            String sku,
            String name,
            Boolean active
    ) {}

    public record CatalogProductImpactResponse(
            Long productId,
            String name,
            String governanceStatus,
            Boolean blockNewStoreAdoption,
            Boolean blockTransactions,
            long linkedStoreCount,
            List<CatalogProductImpactStoreResponse> linkedStores
    ) {}

    public record PlatformIncidentResponse(
            Long id,
            String incidentNumber,
            Long organizationId,
            String subjectType,
            String incidentType,
            String severity,
            String status,
            String title,
            String description,
            Long productId,
            Long storeProductId,
            Long serviceTicketId,
            Long warrantyClaimId,
            String reportedBy,
            String recommendedAction,
            String actionTaken,
            String resolutionNotes,
            LocalDateTime openedAt,
            LocalDateTime resolvedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record CreatePlatformIncidentRequest(
            Long organizationId,
            @NotBlank String subjectType,
            @NotBlank String incidentType,
            @NotBlank String severity,
            @NotBlank String title,
            String description,
            Long productId,
            Long storeProductId,
            Long serviceTicketId,
            Long warrantyClaimId,
            String reportedBy,
            String recommendedAction
    ) {}

    public record UpdatePlatformIncidentStatusRequest(
            @NotBlank String status,
            String actionTaken,
            String resolutionNotes
    ) {}

    public record ApplyIncidentGovernanceActionRequest(
            @NotBlank String actionType,
            String governanceReason,
            String resolutionNotes,
            String incidentStatus
    ) {}

    public record IncidentGovernanceActionResponse(
            PlatformIncidentResponse incident,
            CatalogProductGovernanceResponse product
    ) {}

    public record AuditActivityResponse(
            Long id,
            Long organizationId,
            Long branchId,
            String eventType,
            String entityType,
            Long entityId,
            String entityNumber,
            String action,
            Long actorUserId,
            String actorNameSnapshot,
            String actorRoleSnapshot,
            String summary,
            LocalDateTime occurredAt
    ) {}

    public record SystemHealthResponse(
            long totalNotifications,
            long pendingNotifications,
            long failedNotifications,
            long unreadNotifications,
            long activeReportSchedules,
            long totalAuditEvents,
            List<CountByLabel> notificationsByStatus,
            List<CountByLabel> notificationsByType
    ) {}

    public record CountByLabel(String label, long count) {}

    public record SubscriptionCountByPlan(String planCode, String planName, long count) {}

    public record StoreUpsertRequest(
            @NotBlank String name,
            @NotBlank String code,
            String legalName,
            String phone,
            String email,
            String gstin,
            @NotNull Long ownerAccountId,
            BigDecimal gstThresholdAmount,
            Boolean gstThresholdAlertEnabled,
            Boolean isActive
    ) {}

    public record StoreOnboardingStoreRequest(
            @NotBlank String name,
            @NotBlank String code,
            String legalName,
            String phone,
            String email,
            String gstin,
            BigDecimal gstThresholdAmount,
            Boolean gstThresholdAlertEnabled,
            Boolean isActive
    ) {}

    public record OwnerAccountCreateRequest(
            @NotBlank String loginIdentifier,
            @NotBlank String password,
            @NotBlank String fullName,
            String email,
            String phone,
            Boolean active
    ) {}

    public record StoreBranchSeedRequest(
            @NotBlank String code,
            @NotBlank String name,
            String phone,
            String email,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            Boolean isActive
    ) {}

    public record StoreOnboardingRequest(
            @NotNull @Valid OwnerAccountCreateRequest owner,
            @NotNull @Valid StoreOnboardingStoreRequest store,
            @Valid StoreBranchSeedRequest branch,
            @Valid SubscriptionDtos.ActivateOrganizationSubscriptionRequest subscription
    ) {}

    public record StoreOnboardingResponse(
            StoreSummaryResponse store,
            OwnerAccountReferenceResponse ownerAccount,
            BranchDtos.BranchResponse defaultBranch,
            SubscriptionSummaryResponse subscription
    ) {}

    public record StoreStatusUpdateRequest(
            @NotNull Boolean active
    ) {}

    public record TeamMemberUpdateRequest(
            String fullName,
            String email,
            String phone,
            String roleCode,
            String employeeCode,
            Long defaultBranchId,
            List<Long> branchIds,
            Boolean active
    ) {}

    public record TeamMemberStatusUpdateRequest(
            @NotNull Boolean active
    ) {}

    public record SubscriptionPlanUpsertRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotBlank String billingPeriod,
            Integer maxOrganizations,
            Boolean unlimitedOrganizations,
            Boolean active
    ) {}

    public record SubscriptionPlanFeatureAssignmentRequest(
            @NotBlank String featureCode,
            Boolean enabled,
            Integer featureLimit,
            String configJson
    ) {}

    public record UpdateSubscriptionPlanFeaturesRequest(
            @NotEmpty List<@Valid SubscriptionPlanFeatureAssignmentRequest> items
    ) {}
}
