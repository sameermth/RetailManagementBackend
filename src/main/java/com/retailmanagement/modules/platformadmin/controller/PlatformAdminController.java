package com.retailmanagement.modules.platformadmin.controller;

import com.retailmanagement.modules.auth.dto.response.EmployeeManagementResponses;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.service.dto.ErpServiceDtos;
import com.retailmanagement.modules.erp.subscription.dto.SubscriptionDtos;
import com.retailmanagement.modules.platformadmin.dto.PlatformAdminDtos;
import com.retailmanagement.modules.platformadmin.service.PlatformAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform-admin")
@RequiredArgsConstructor
@Tag(name = "Platform Admin", description = "Cross-store platform admin overview and operational endpoints")
public class PlatformAdminController {

    private static final String PLATFORM_ADMIN_AUTHORITY = "hasAuthority('platform.manage')";

    private final PlatformAdminService platformAdminService;

    @GetMapping("/overview")
    @Operation(summary = "Get platform overview")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.OverviewResponse> overview() {
        return ErpApiResponse.ok(platformAdminService.overview());
    }

    @GetMapping("/stores")
    @Operation(summary = "List all stores on the platform")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<PlatformAdminDtos.StoreSummaryResponse>> stores() {
        return ErpApiResponse.ok(platformAdminService.stores());
    }

    @GetMapping("/catalog/products")
    @Operation(summary = "List shared catalog products with governance state")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<PlatformAdminDtos.CatalogProductGovernanceResponse>> catalogProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String governanceStatus
    ) {
        return ErpApiResponse.ok(platformAdminService.catalogProducts(query, governanceStatus));
    }

    @GetMapping("/catalog/products/{productId}/impact")
    @Operation(summary = "Get store impact for a governed catalog product")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.CatalogProductImpactResponse> catalogProductImpact(
            @PathVariable Long productId
    ) {
        return ErpApiResponse.ok(platformAdminService.catalogProductImpact(productId));
    }

    @PutMapping("/catalog/products/{productId}/governance")
    @Operation(summary = "Update shared product governance state")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.CatalogProductGovernanceResponse> updateCatalogProductGovernance(
            @PathVariable Long productId,
            @RequestBody @Valid PlatformAdminDtos.UpdateProductGovernanceRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.updateProductGovernance(productId, request), "Catalog product governance updated");
    }

    @GetMapping("/owner-accounts")
    @Operation(summary = "List owner accounts for store assignment")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<PlatformAdminDtos.OwnerAccountReferenceResponse>> ownerAccounts(
            @RequestParam(required = false) String query
    ) {
        return ErpApiResponse.ok(platformAdminService.ownerAccounts(query));
    }

    @GetMapping("/stores/{organizationId}")
    @Operation(summary = "Get store by organization id")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.StoreSummaryResponse> getStore(@PathVariable Long organizationId) {
        return ErpApiResponse.ok(platformAdminService.getStore(organizationId));
    }

    @PostMapping("/stores")
    @Operation(summary = "Create store from platform admin")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.StoreSummaryResponse> createStore(
            @RequestBody @Valid PlatformAdminDtos.StoreUpsertRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.createStore(request), "Platform store created");
    }

    @PostMapping("/stores/onboard")
    @Operation(summary = "Onboard a new store with owner account, branch, and optional subscription")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.StoreOnboardingResponse> onboardStore(
            @RequestBody @Valid PlatformAdminDtos.StoreOnboardingRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.onboardStore(request), "Platform store onboarded");
    }

    @PutMapping("/stores/{organizationId}")
    @Operation(summary = "Update store from platform admin")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.StoreSummaryResponse> updateStore(
            @PathVariable Long organizationId,
            @RequestBody @Valid PlatformAdminDtos.StoreUpsertRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.updateStore(organizationId, request), "Platform store updated");
    }

    @PutMapping("/stores/{organizationId}/status")
    @Operation(summary = "Activate or deactivate store")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.StoreSummaryResponse> updateStoreStatus(
            @PathVariable Long organizationId,
            @RequestBody @Valid PlatformAdminDtos.StoreStatusUpdateRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.updateStoreStatus(organizationId, request.active()), "Store status updated");
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "List cross-store subscription summaries")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<PlatformAdminDtos.SubscriptionSummaryResponse>> subscriptions() {
        return ErpApiResponse.ok(platformAdminService.subscriptions());
    }

    @PostMapping("/subscriptions/organizations/{organizationId}/change-plan")
    @Operation(summary = "Change store owner subscription plan from platform admin")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.SubscriptionSummaryResponse> changeSubscriptionPlan(
            @PathVariable Long organizationId,
            @RequestBody @Valid SubscriptionDtos.ActivateOrganizationSubscriptionRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.changeSubscriptionPlan(organizationId, request), "Platform subscription updated");
    }

    @PostMapping("/subscriptions/organizations/{organizationId}/cancel")
    @Operation(summary = "Cancel store owner subscription from platform admin")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.SubscriptionSummaryResponse> cancelSubscription(
            @PathVariable Long organizationId,
            @RequestBody @Valid SubscriptionDtos.CancelOrganizationSubscriptionRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.cancelSubscription(organizationId, request), "Platform subscription cancelled");
    }

    @GetMapping("/plans-features")
    @Operation(summary = "List subscription plans and enabled feature matrix")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<SubscriptionDtos.SubscriptionPlanResponse>> plansFeatures() {
        return ErpApiResponse.ok(platformAdminService.plansFeatures());
    }

    @PostMapping("/plans-features/plans")
    @Operation(summary = "Create subscription plan")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<SubscriptionDtos.SubscriptionPlanResponse> createPlan(
            @RequestBody @Valid PlatformAdminDtos.SubscriptionPlanUpsertRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.createPlan(request), "Subscription plan created");
    }

    @PutMapping("/plans-features/plans/{planId}")
    @Operation(summary = "Update subscription plan")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<SubscriptionDtos.SubscriptionPlanResponse> updatePlan(
            @PathVariable Long planId,
            @RequestBody @Valid PlatformAdminDtos.SubscriptionPlanUpsertRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.updatePlan(planId, request), "Subscription plan updated");
    }

    @PutMapping("/plans-features/plans/{planId}/features")
    @Operation(summary = "Replace subscription plan feature assignments")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<SubscriptionDtos.SubscriptionPlanResponse> updatePlanFeatures(
            @PathVariable Long planId,
            @RequestBody @Valid PlatformAdminDtos.UpdateSubscriptionPlanFeaturesRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.updatePlanFeatures(planId, request), "Subscription plan features updated");
    }

    @GetMapping("/store-teams")
    @Operation(summary = "List team members across stores")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<PlatformAdminDtos.TeamMemberSummaryResponse>> storeTeams() {
        return ErpApiResponse.ok(platformAdminService.storeTeams());
    }

    @GetMapping("/store-teams/{userId}")
    @Operation(summary = "Get a team member within a store")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<EmployeeManagementResponses.EmployeeResponse> getStoreTeamMember(
            @PathVariable Long userId,
            @RequestParam Long organizationId
    ) {
        return ErpApiResponse.ok(platformAdminService.getStoreTeamMember(organizationId, userId));
    }

    @PutMapping("/store-teams/{userId}")
    @Operation(summary = "Update a team member within a store")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<EmployeeManagementResponses.EmployeeResponse> updateStoreTeamMember(
            @PathVariable Long userId,
            @RequestParam Long organizationId,
            @RequestBody @Valid PlatformAdminDtos.TeamMemberUpdateRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.updateStoreTeamMember(organizationId, userId, request), "Store team member updated");
    }

    @PutMapping("/store-teams/{userId}/status")
    @Operation(summary = "Activate or deactivate a team member")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<EmployeeManagementResponses.EmployeeResponse> updateStoreTeamMemberStatus(
            @PathVariable Long userId,
            @RequestParam Long organizationId,
            @RequestBody @Valid PlatformAdminDtos.TeamMemberStatusUpdateRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.updateStoreTeamMemberStatus(organizationId, userId, request.active()), "Store team member status updated");
    }

    @GetMapping("/support-grievances")
    @Operation(summary = "List support and grievance items across stores")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<PlatformAdminDtos.SupportItemSummaryResponse>> supportGrievances() {
        return ErpApiResponse.ok(platformAdminService.supportGrievances());
    }

    @PostMapping("/support-grievances/service-tickets/{ticketId}/assign")
    @Operation(summary = "Assign a service ticket from platform admin")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<ErpServiceDtos.ServiceTicketResponse> assignSupportTicket(
            @PathVariable Long ticketId,
            @RequestBody @Valid ErpServiceDtos.AssignServiceTicketRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.assignSupportTicket(ticketId, request), "Support ticket assigned");
    }

    @PostMapping("/support-grievances/service-tickets/{ticketId}/close")
    @Operation(summary = "Close a service ticket from platform admin")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<ErpServiceDtos.ServiceTicketResponse> closeSupportTicket(
            @PathVariable Long ticketId,
            @RequestBody @Valid ErpServiceDtos.CloseServiceTicketRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.closeSupportTicket(ticketId, request), "Support ticket closed");
    }

    @PostMapping("/support-grievances/warranty-claims/{claimId}/status")
    @Operation(summary = "Update warranty claim status from platform admin")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<ErpServiceDtos.WarrantyClaimResponse> updateWarrantyClaimStatus(
            @PathVariable Long claimId,
            @RequestBody @Valid ErpServiceDtos.UpdateWarrantyClaimStatusRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.updateWarrantyClaimStatus(claimId, request), "Warranty claim status updated");
    }

    @GetMapping("/feedback")
    @Operation(summary = "List feedback captured across stores")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<PlatformAdminDtos.FeedbackSummaryResponse>> feedback() {
        return ErpApiResponse.ok(platformAdminService.feedback());
    }

    @GetMapping("/incidents")
    @Operation(summary = "List platform incidents and quality complaints")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<PlatformAdminDtos.PlatformIncidentResponse>> incidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String subjectType,
            @RequestParam(required = false) Long organizationId
    ) {
        return ErpApiResponse.ok(platformAdminService.incidents(status, subjectType, organizationId));
    }

    @PostMapping("/incidents")
    @Operation(summary = "Create platform incident or complaint")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.PlatformIncidentResponse> createIncident(
            @RequestBody @Valid PlatformAdminDtos.CreatePlatformIncidentRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.createIncident(request), "Platform incident created");
    }

    @PutMapping("/incidents/{incidentId}/status")
    @Operation(summary = "Update platform incident status")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.PlatformIncidentResponse> updateIncidentStatus(
            @PathVariable Long incidentId,
            @RequestBody @Valid PlatformAdminDtos.UpdatePlatformIncidentStatusRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.updateIncidentStatus(incidentId, request), "Platform incident updated");
    }

    @PostMapping("/incidents/{incidentId}/apply-governance")
    @Operation(summary = "Apply catalog governance action from a platform incident")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.IncidentGovernanceActionResponse> applyIncidentGovernance(
            @PathVariable Long incidentId,
            @RequestBody @Valid PlatformAdminDtos.ApplyIncidentGovernanceActionRequest request
    ) {
        return ErpApiResponse.ok(platformAdminService.applyIncidentGovernanceAction(incidentId, request), "Platform incident governance applied");
    }

    @GetMapping("/notifications")
    @Operation(summary = "List recent platform notification activity")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<PlatformAdminDtos.NotificationSummaryResponse>> notifications() {
        return ErpApiResponse.ok(platformAdminService.notifications());
    }

    @GetMapping("/audit-activity")
    @Operation(summary = "List recent platform audit activity")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<List<PlatformAdminDtos.AuditActivityResponse>> auditActivity() {
        return ErpApiResponse.ok(platformAdminService.auditActivity());
    }

    @GetMapping("/system-health")
    @Operation(summary = "Get platform operational health summary")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.SystemHealthResponse> systemHealth() {
        return ErpApiResponse.ok(platformAdminService.systemHealth());
    }

    @GetMapping("/reports")
    @Operation(summary = "Get platform-level summary reports")
    @PreAuthorize(PLATFORM_ADMIN_AUTHORITY)
    public ErpApiResponse<PlatformAdminDtos.PlatformReportsResponse> reports() {
        return ErpApiResponse.ok(platformAdminService.reports());
    }
}
