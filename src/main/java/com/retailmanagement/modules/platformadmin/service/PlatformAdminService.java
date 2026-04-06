package com.retailmanagement.modules.platformadmin.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.auth.dto.response.EmployeeManagementResponses;
import com.retailmanagement.modules.auth.model.Account;
import com.retailmanagement.modules.auth.model.OrganizationPersonProfile;
import com.retailmanagement.modules.auth.model.Role;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.model.UserBranchAccess;
import com.retailmanagement.modules.auth.repository.AccountRepository;
import com.retailmanagement.modules.auth.repository.OrganizationPersonProfileRepository;
import com.retailmanagement.modules.auth.repository.RoleRepository;
import com.retailmanagement.modules.auth.repository.UserBranchAccessRepository;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.erp.audit.entity.AuditEvent;
import com.retailmanagement.modules.erp.audit.repository.AuditEventRepository;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.foundation.repository.WarehouseRepository;
import com.retailmanagement.modules.erp.service.dto.ErpServiceDtos;
import com.retailmanagement.modules.erp.service.entity.ServiceTicket;
import com.retailmanagement.modules.erp.service.entity.ServiceVisit;
import com.retailmanagement.modules.erp.service.entity.WarrantyClaim;
import com.retailmanagement.modules.erp.service.repository.ServiceTicketRepository;
import com.retailmanagement.modules.erp.service.repository.ServiceVisitRepository;
import com.retailmanagement.modules.erp.service.repository.WarrantyClaimRepository;
import com.retailmanagement.modules.erp.service.service.ErpServiceWarrantyService;
import com.retailmanagement.modules.erp.subscription.entity.AccountSubscription;
import com.retailmanagement.modules.erp.subscription.entity.SubscriptionFeature;
import com.retailmanagement.modules.erp.subscription.entity.SubscriptionPlan;
import com.retailmanagement.modules.erp.subscription.entity.SubscriptionPlanFeature;
import com.retailmanagement.modules.erp.subscription.dto.SubscriptionDtos;
import com.retailmanagement.modules.erp.subscription.repository.AccountSubscriptionRepository;
import com.retailmanagement.modules.erp.subscription.repository.SubscriptionFeatureRepository;
import com.retailmanagement.modules.erp.subscription.repository.SubscriptionPlanFeatureRepository;
import com.retailmanagement.modules.erp.subscription.repository.SubscriptionPlanRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionManagementService;
import com.retailmanagement.modules.notification.model.Notification;
import com.retailmanagement.modules.notification.repository.NotificationRepository;
import com.retailmanagement.modules.report.model.ReportSchedule;
import com.retailmanagement.modules.report.repository.ReportScheduleRepository;
import com.retailmanagement.modules.platformadmin.dto.PlatformAdminDtos;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformAdminService {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final OrganizationPersonProfileRepository organizationPersonProfileRepository;
    private final UserBranchAccessRepository userBranchAccessRepository;
    private final SubscriptionManagementService subscriptionManagementService;
    private final AccountSubscriptionRepository accountSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionFeatureRepository subscriptionFeatureRepository;
    private final SubscriptionPlanFeatureRepository subscriptionPlanFeatureRepository;
    private final ErpServiceWarrantyService erpServiceWarrantyService;
    private final ServiceTicketRepository serviceTicketRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final ServiceVisitRepository serviceVisitRepository;
    private final ReportScheduleRepository reportScheduleRepository;
    private final NotificationRepository notificationRepository;
    private final AuditEventRepository auditEventRepository;

    public PlatformAdminDtos.OverviewResponse overview() {
        List<Organization> organizations = organizationRepository.findAll();
        List<User> users = allUsersByOrganization(organizations);
        List<AccountSubscription> currentSubscriptions = currentSubscriptionsByOwner(organizations).values().stream()
                .filter(Objects::nonNull)
                .toList();
        List<PlatformAdminDtos.SupportItemSummaryResponse> supportItems = supportGrievances();
        List<PlatformAdminDtos.FeedbackSummaryResponse> feedbackItems = feedback();

        Set<Long> ownerAccounts = organizations.stream()
                .map(Organization::getOwnerAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new PlatformAdminDtos.OverviewResponse(
                organizations.size(),
                organizations.stream().filter(org -> Boolean.TRUE.equals(org.getIsActive())).count(),
                ownerAccounts.size(),
                users.size(),
                users.stream().filter(user -> Boolean.TRUE.equals(user.getActive())).count(),
                supportItems.stream().filter(item -> !isClosedStatus(item.status())).count(),
                feedbackItems.size(),
                reportScheduleRepository.findByIsActiveTrue().size(),
                summarizeSubscriptionsByPlan(currentSubscriptions),
                summarizeUsersByRole(users)
        );
    }

    public List<PlatformAdminDtos.StoreSummaryResponse> stores() {
        List<Organization> organizations = organizationRepository.findAll().stream()
                .sorted(Comparator.comparing(Organization::getId))
                .toList();
        return organizations.stream()
                .map(this::toStoreSummary)
                .toList();
    }

    public PlatformAdminDtos.StoreSummaryResponse getStore(Long organizationId) {
        return toStoreSummary(requireOrganization(organizationId));
    }

    @Transactional
    public PlatformAdminDtos.StoreSummaryResponse createStore(PlatformAdminDtos.StoreUpsertRequest request) {
        String code = request.code().trim().toUpperCase();
        if (organizationRepository.findByCode(code).isPresent()) {
            throw new BusinessException("Organization code already exists: " + code);
        }
        Account ownerAccount = accountRepository.findById(request.ownerAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner account not found: " + request.ownerAccountId()));
        Organization organization = new Organization();
        organization.setName(request.name().trim());
        organization.setCode(code);
        organization.setLegalName(trimToNull(request.legalName()));
        organization.setPhone(trimToNull(request.phone()));
        organization.setEmail(trimToNull(request.email()));
        organization.setGstin(trimToNull(request.gstin()));
        organization.setOwnerAccountId(ownerAccount.getId());
        if (request.gstThresholdAmount() != null) {
            organization.setGstThresholdAmount(request.gstThresholdAmount());
        }
        if (request.gstThresholdAlertEnabled() != null) {
            organization.setGstThresholdAlertEnabled(request.gstThresholdAlertEnabled());
        }
        if (request.isActive() != null) {
            organization.setIsActive(request.isActive());
        }
        Organization saved = organizationRepository.save(organization);
        ensureOwnerMembership(saved, ownerAccount);
        return toStoreSummary(saved);
    }

    @Transactional
    public PlatformAdminDtos.StoreSummaryResponse updateStore(Long organizationId, PlatformAdminDtos.StoreUpsertRequest request) {
        Organization organization = requireOrganization(organizationId);
        String code = request.code().trim().toUpperCase();
        organizationRepository.findByCode(code)
                .filter(existing -> !existing.getId().equals(organizationId))
                .ifPresent(existing -> {
                    throw new BusinessException("Organization code already exists: " + code);
                });
        Account ownerAccount = accountRepository.findById(request.ownerAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner account not found: " + request.ownerAccountId()));
        organization.setName(request.name().trim());
        organization.setCode(code);
        organization.setLegalName(trimToNull(request.legalName()));
        organization.setPhone(trimToNull(request.phone()));
        organization.setEmail(trimToNull(request.email()));
        organization.setGstin(trimToNull(request.gstin()));
        organization.setOwnerAccountId(request.ownerAccountId());
        if (request.gstThresholdAmount() != null) {
            organization.setGstThresholdAmount(request.gstThresholdAmount());
        }
        if (request.gstThresholdAlertEnabled() != null) {
            organization.setGstThresholdAlertEnabled(request.gstThresholdAlertEnabled());
        }
        if (request.isActive() != null) {
            organization.setIsActive(request.isActive());
        }
        Organization saved = organizationRepository.save(organization);
        ensureOwnerMembership(saved, ownerAccount);
        return toStoreSummary(saved);
    }

    @Transactional
    public PlatformAdminDtos.StoreSummaryResponse updateStoreStatus(Long organizationId, boolean active) {
        Organization organization = requireOrganization(organizationId);
        organization.setIsActive(active);
        return toStoreSummary(organizationRepository.save(organization));
    }

    public List<PlatformAdminDtos.SubscriptionSummaryResponse> subscriptions() {
        List<Organization> organizations = organizationRepository.findAll().stream()
                .sorted(Comparator.comparing(Organization::getId))
                .toList();
        return organizations.stream()
                .map(this::toSubscriptionSummary)
                .toList();
    }

    @Transactional
    public PlatformAdminDtos.SubscriptionSummaryResponse changeSubscriptionPlan(
            Long organizationId,
            SubscriptionDtos.ActivateOrganizationSubscriptionRequest request
    ) {
        subscriptionManagementService.changePlan(organizationId, request);
        return toSubscriptionSummary(requireOrganization(organizationId));
    }

    @Transactional
    public PlatformAdminDtos.SubscriptionSummaryResponse cancelSubscription(
            Long organizationId,
            SubscriptionDtos.CancelOrganizationSubscriptionRequest request
    ) {
        subscriptionManagementService.cancelSubscription(organizationId, request);
        return toSubscriptionSummary(requireOrganization(organizationId));
    }

    public List<PlatformAdminDtos.TeamMemberSummaryResponse> storeTeams() {
        return organizationRepository.findAll().stream()
                .sorted(Comparator.comparing(Organization::getId))
                .flatMap(org -> userRepository.findByOrganizationIdOrderByIdAsc(org.getId()).stream()
                        .map(user -> toTeamMember(org, user)))
                .sorted(Comparator.comparing(PlatformAdminDtos.TeamMemberSummaryResponse::organizationId)
                        .thenComparing(PlatformAdminDtos.TeamMemberSummaryResponse::userId))
                .toList();
    }

    public EmployeeManagementResponses.EmployeeResponse getStoreTeamMember(Long organizationId, Long userId) {
        return toEmployeeResponse(requireUser(organizationId, userId));
    }

    @Transactional
    public EmployeeManagementResponses.EmployeeResponse updateStoreTeamMember(
            Long organizationId,
            Long userId,
            PlatformAdminDtos.TeamMemberUpdateRequest request
    ) {
        User user = requireUser(organizationId, userId);
        if (request.fullName() != null && user.getPerson() != null) {
            user.getPerson().setLegalName(request.fullName().trim());
        }
        if (request.email() != null) {
            if (user.getPerson() != null) user.getPerson().setPrimaryEmail(request.email());
            if (user.getOrganizationPersonProfile() != null) user.getOrganizationPersonProfile().setEmailForOrg(request.email());
        }
        if (request.phone() != null) {
            if (user.getPerson() != null) user.getPerson().setPrimaryPhone(request.phone());
            if (user.getOrganizationPersonProfile() != null) user.getOrganizationPersonProfile().setPhoneForOrg(request.phone());
        }
        if (request.fullName() != null && user.getOrganizationPersonProfile() != null) {
            user.getOrganizationPersonProfile().setDisplayName(request.fullName().trim());
        }
        if (request.roleCode() != null) {
            user.setRole(resolveRole(request.roleCode()));
        }
        if (request.employeeCode() != null) {
            user.setEmployeeCode(resolveEmployeeCode(organizationId, userId, request.employeeCode()));
        }
        if (request.defaultBranchId() != null || request.branchIds() != null) {
            List<Long> branchIds = request.branchIds() == null
                    ? userBranchAccessRepository.findByUserId(userId).stream().map(UserBranchAccess::getBranchId).toList()
                    : request.branchIds();
            Long defaultBranchId = request.defaultBranchId() != null ? request.defaultBranchId() : user.getDefaultBranchId();
            validateBranches(organizationId, branchIds, defaultBranchId);
            user.setDefaultBranchId(defaultBranchId);
            replaceBranchAccess(userId, branchIds, defaultBranchId);
        }
        if (request.active() != null) {
            user.setActive(request.active());
            if (user.getAccount() != null) {
                user.getAccount().setActive(request.active());
            }
        }
        userRepository.save(user);
        return toEmployeeResponse(requireUser(organizationId, userId));
    }

    @Transactional
    public EmployeeManagementResponses.EmployeeResponse updateStoreTeamMemberStatus(Long organizationId, Long userId, boolean active) {
        User user = requireUser(organizationId, userId);
        user.setActive(active);
        if (user.getAccount() != null) {
            user.getAccount().setActive(active);
        }
        userRepository.save(user);
        return toEmployeeResponse(requireUser(organizationId, userId));
    }

    public List<PlatformAdminDtos.SupportItemSummaryResponse> supportGrievances() {
        List<PlatformAdminDtos.SupportItemSummaryResponse> items = new ArrayList<>();
        for (Organization organization : organizationRepository.findAll().stream().sorted(Comparator.comparing(Organization::getId)).toList()) {
            for (ServiceTicket ticket : serviceTicketRepository.findTop100ByOrganizationIdOrderByReportedOnDescIdDesc(organization.getId())) {
                items.add(new PlatformAdminDtos.SupportItemSummaryResponse(
                        "SERVICE_TICKET",
                        organization.getId(),
                        organization.getCode(),
                        organization.getName(),
                        ticket.getBranchId(),
                        ticket.getId(),
                        ticket.getTicketNumber(),
                        ticket.getStatus(),
                        ticket.getPriority(),
                        ticket.getComplaintSummary(),
                        ticket.getReportedOn()
                ));
            }
            for (WarrantyClaim claim : warrantyClaimRepository.findTop100ByOrganizationIdOrderByClaimDateDescIdDesc(organization.getId())) {
                items.add(new PlatformAdminDtos.SupportItemSummaryResponse(
                        "WARRANTY_CLAIM",
                        organization.getId(),
                        organization.getCode(),
                        organization.getName(),
                        claim.getBranchId(),
                        claim.getId(),
                        claim.getClaimNumber(),
                        claim.getStatus(),
                        claim.getClaimType(),
                        claim.getClaimNotes(),
                        claim.getClaimDate()
                ));
            }
        }
        return items.stream()
                .sorted(Comparator.comparing(PlatformAdminDtos.SupportItemSummaryResponse::eventDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PlatformAdminDtos.SupportItemSummaryResponse::referenceId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public ErpServiceDtos.ServiceTicketResponse assignSupportTicket(Long ticketId, ErpServiceDtos.AssignServiceTicketRequest request) {
        if (request.organizationId() == null || request.branchId() == null) {
            throw new BusinessException("organizationId and branchId are required");
        }
        return toTicketResponse(erpServiceWarrantyService.assignTicket(request.organizationId(), request.branchId(), ticketId, request));
    }

    @Transactional
    public ErpServiceDtos.ServiceTicketResponse closeSupportTicket(Long ticketId, ErpServiceDtos.CloseServiceTicketRequest request) {
        if (request.organizationId() == null || request.branchId() == null) {
            throw new BusinessException("organizationId and branchId are required");
        }
        return toTicketResponse(erpServiceWarrantyService.closeTicket(request.organizationId(), request.branchId(), ticketId, request));
    }

    @Transactional
    public ErpServiceDtos.WarrantyClaimResponse updateWarrantyClaimStatus(Long claimId, ErpServiceDtos.UpdateWarrantyClaimStatusRequest request) {
        if (request.organizationId() == null || request.branchId() == null) {
            throw new BusinessException("organizationId and branchId are required");
        }
        return toClaimResponse(erpServiceWarrantyService.updateClaimStatus(request.organizationId(), request.branchId(), claimId, request));
    }

    public List<PlatformAdminDtos.FeedbackSummaryResponse> feedback() {
        List<PlatformAdminDtos.FeedbackSummaryResponse> feedback = new ArrayList<>();
        for (Organization organization : organizationRepository.findAll().stream().sorted(Comparator.comparing(Organization::getId)).toList()) {
            for (ServiceVisit visit : serviceVisitRepository.findTop100ByOrganizationIdOrderByScheduledAtDescIdDesc(organization.getId())) {
                if (visit.getCustomerFeedback() == null || visit.getCustomerFeedback().isBlank()) {
                    continue;
                }
                feedback.add(new PlatformAdminDtos.FeedbackSummaryResponse(
                        organization.getId(),
                        organization.getCode(),
                        organization.getName(),
                        visit.getId(),
                        visit.getServiceTicketId(),
                        visit.getVisitStatus(),
                        visit.getScheduledAt(),
                        visit.getCompletedAt(),
                        visit.getCustomerFeedback()
                ));
            }
        }
        return feedback.stream()
                .sorted(Comparator.comparing(PlatformAdminDtos.FeedbackSummaryResponse::completedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PlatformAdminDtos.FeedbackSummaryResponse::serviceVisitId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public PlatformAdminDtos.PlatformReportsResponse reports() {
        List<Organization> organizations = organizationRepository.findAll();
        List<User> users = allUsersByOrganization(organizations);
        List<AccountSubscription> currentSubscriptions = currentSubscriptionsByOwner(organizations).values().stream()
                .filter(Objects::nonNull)
                .toList();
        List<PlatformAdminDtos.SupportItemSummaryResponse> supportItems = supportGrievances();
        List<PlatformAdminDtos.FeedbackSummaryResponse> feedbackItems = feedback();
        return new PlatformAdminDtos.PlatformReportsResponse(
                summarizeStoresByStatus(organizations),
                summarizeSubscriptionsByPlan(currentSubscriptions),
                summarizeUsersByRole(users),
                summarizeSupportByStatus(supportItems),
                summarizeFeedbackByVisitStatus(feedbackItems),
                reportScheduleRepository.findByIsActiveTrue().size()
        );
    }

    public List<SubscriptionDtos.SubscriptionPlanResponse> plansFeatures() {
        Map<Long, List<SubscriptionPlanFeature>> featuresByPlanId = subscriptionPlanFeatureRepository.findAll().stream()
                .collect(Collectors.groupingBy(planFeature -> planFeature.getPlan().getId(), LinkedHashMap::new, Collectors.toList()));
        return subscriptionPlanRepository.findAll().stream()
                .sorted(Comparator.comparing(SubscriptionPlan::getId))
                .map(plan -> toPlanResponse(plan, featuresByPlanId.getOrDefault(plan.getId(), List.of())))
                .toList();
    }

    @Transactional
    public SubscriptionDtos.SubscriptionPlanResponse createPlan(PlatformAdminDtos.SubscriptionPlanUpsertRequest request) {
        String code = request.code().trim().toUpperCase();
        if (subscriptionPlanRepository.findByCode(code).isPresent()) {
            throw new BusinessException("Subscription plan code already exists: " + code);
        }
        SubscriptionPlan plan = new SubscriptionPlan();
        applyPlanChanges(plan, request);
        return toPlanResponse(subscriptionPlanRepository.save(plan), List.of());
    }

    @Transactional
    public SubscriptionDtos.SubscriptionPlanResponse updatePlan(Long planId, PlatformAdminDtos.SubscriptionPlanUpsertRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + planId));
        String code = request.code().trim().toUpperCase();
        subscriptionPlanRepository.findByCode(code)
                .filter(existing -> !existing.getId().equals(planId))
                .ifPresent(existing -> {
                    throw new BusinessException("Subscription plan code already exists: " + code);
                });
        applyPlanChanges(plan, request);
        return toPlanResponse(subscriptionPlanRepository.save(plan), subscriptionPlanFeatureRepository.findByPlanIdOrderByIdAsc(planId));
    }

    @Transactional
    public SubscriptionDtos.SubscriptionPlanResponse updatePlanFeatures(
            Long planId,
            PlatformAdminDtos.UpdateSubscriptionPlanFeaturesRequest request
    ) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + planId));
        Map<String, SubscriptionPlanFeature> existingByFeatureCode = subscriptionPlanFeatureRepository.findByPlanIdOrderByIdAsc(planId).stream()
                .collect(Collectors.toMap(
                        planFeature -> planFeature.getFeature().getCode(),
                        planFeature -> planFeature,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<String> requestedCodes = request.items().stream()
                .map(item -> item.featureCode().trim().toLowerCase())
                .distinct()
                .toList();
        Map<String, SubscriptionFeature> featuresByCode = subscriptionFeatureRepository.findByCodeIn(requestedCodes).stream()
                .collect(Collectors.toMap(SubscriptionFeature::getCode, feature -> feature));
        for (String featureCode : requestedCodes) {
            if (!featuresByCode.containsKey(featureCode)) {
                throw new ResourceNotFoundException("Subscription feature not found: " + featureCode);
            }
        }
        for (PlatformAdminDtos.SubscriptionPlanFeatureAssignmentRequest item : request.items()) {
            String featureCode = item.featureCode().trim().toLowerCase();
            SubscriptionPlanFeature planFeature = existingByFeatureCode.get(featureCode);
            if (planFeature == null) {
                planFeature = new SubscriptionPlanFeature();
                planFeature.setPlan(plan);
                planFeature.setFeature(featuresByCode.get(featureCode));
            }
            planFeature.setIsEnabled(item.enabled() == null || item.enabled());
            planFeature.setFeatureLimit(item.featureLimit());
            planFeature.setConfigJson(item.configJson() == null || item.configJson().isBlank() ? "{}" : item.configJson().trim());
            subscriptionPlanFeatureRepository.save(planFeature);
        }
        return toPlanResponse(plan, subscriptionPlanFeatureRepository.findByPlanIdOrderByIdAsc(planId));
    }

    public List<PlatformAdminDtos.NotificationSummaryResponse> notifications() {
        return notificationRepository.findTop200ByOrderByCreatedAtDescIdDesc().stream()
                .map(notification -> new PlatformAdminDtos.NotificationSummaryResponse(
                        notification.getId(),
                        notification.getNotificationId(),
                        notification.getType() == null ? null : notification.getType().name(),
                        notification.getChannel() == null ? null : notification.getChannel().name(),
                        notification.getStatus() == null ? null : notification.getStatus().name(),
                        notification.getPriority() == null ? null : notification.getPriority().name(),
                        notification.getTitle(),
                        notification.getRecipient(),
                        notification.getReferenceType(),
                        notification.getReferenceId(),
                        notification.getScheduledFor(),
                        notification.getSentAt(),
                        notification.getCreatedAt(),
                        notification.getRetryCount(),
                        notification.getErrorMessage()
                ))
                .toList();
    }

    public List<PlatformAdminDtos.AuditActivityResponse> auditActivity() {
        return auditEventRepository.findTop200ByOrderByOccurredAtDescIdDesc().stream()
                .map(event -> new PlatformAdminDtos.AuditActivityResponse(
                        event.getId(),
                        event.getOrganizationId(),
                        event.getBranchId(),
                        event.getEventType(),
                        event.getEntityType(),
                        event.getEntityId(),
                        event.getEntityNumber(),
                        event.getAction(),
                        event.getActorUserId(),
                        event.getActorNameSnapshot(),
                        event.getActorRoleSnapshot(),
                        event.getSummary(),
                        event.getOccurredAt()
                ))
                .toList();
    }

    public PlatformAdminDtos.SystemHealthResponse systemHealth() {
        List<Notification> notifications = notificationRepository.findAll();
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        return new PlatformAdminDtos.SystemHealthResponse(
                notifications.size(),
                notifications.stream().filter(n -> n.getStatus() != null && "PENDING".equals(n.getStatus().name())).count(),
                notifications.stream().filter(n -> n.getStatus() != null && "FAILED".equals(n.getStatus().name())).count(),
                notifications.stream().filter(n -> n.getReadAt() == null).count(),
                reportScheduleRepository.findByIsActiveTrue().size(),
                auditEvents.size(),
                notifications.stream()
                        .collect(Collectors.groupingBy(
                                n -> n.getStatus() == null ? "UNKNOWN" : n.getStatus().name(),
                                LinkedHashMap::new,
                                Collectors.counting()))
                        .entrySet().stream()
                        .map(entry -> new PlatformAdminDtos.CountByLabel(entry.getKey(), entry.getValue()))
                        .toList(),
                notifications.stream()
                        .collect(Collectors.groupingBy(
                                n -> n.getType() == null ? "UNKNOWN" : n.getType().name(),
                                LinkedHashMap::new,
                                Collectors.counting()))
                        .entrySet().stream()
                        .map(entry -> new PlatformAdminDtos.CountByLabel(entry.getKey(), entry.getValue()))
                        .toList()
        );
    }

    private PlatformAdminDtos.TeamMemberSummaryResponse toTeamMember(Organization organization, User user) {
        return new PlatformAdminDtos.TeamMemberSummaryResponse(
                organization.getId(),
                organization.getCode(),
                organization.getName(),
                user.getId(),
                user.getAccountId(),
                user.getPersonId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole() == null ? null : user.getRole().getCode(),
                user.getRole() == null ? null : user.getRole().getName(),
                user.getEmployeeCode(),
                user.getDefaultBranchId(),
                user.getActive(),
                user.getCreatedAt()
        );
    }

    private List<User> allUsersByOrganization(List<Organization> organizations) {
        return organizations.stream()
                .flatMap(org -> userRepository.findByOrganizationIdOrderByIdAsc(org.getId()).stream())
                .toList();
    }

    private Map<Long, AccountSubscription> currentSubscriptionsByOwner(List<Organization> organizations) {
        Set<Long> ownerAccountIds = organizations.stream()
                .map(Organization::getOwnerAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, AccountSubscription> subscriptionsByOwner = new LinkedHashMap<>();
        for (Long ownerAccountId : ownerAccountIds) {
            subscriptionsByOwner.put(ownerAccountId, accountSubscriptionRepository.findCurrentSubscription(ownerAccountId, LocalDate.now()).orElse(null));
        }
        return subscriptionsByOwner;
    }

    private Map<Long, Long> organizationsUsedByOwner(List<Organization> organizations) {
        return organizations.stream()
                .filter(org -> org.getOwnerAccountId() != null && Boolean.TRUE.equals(org.getIsActive()))
                .collect(Collectors.groupingBy(Organization::getOwnerAccountId, LinkedHashMap::new, Collectors.counting()));
    }

    private List<PlatformAdminDtos.SubscriptionCountByPlan> summarizeSubscriptionsByPlan(List<AccountSubscription> subscriptions) {
        Map<String, List<AccountSubscription>> byPlan = subscriptions.stream()
                .collect(Collectors.groupingBy(subscription -> subscription.getPlan().getCode(), LinkedHashMap::new, Collectors.toList()));
        return byPlan.entrySet().stream()
                .map(entry -> new PlatformAdminDtos.SubscriptionCountByPlan(
                        entry.getKey(),
                        entry.getValue().getFirst().getPlan().getName(),
                        entry.getValue().size()
                ))
                .toList();
    }

    private SubscriptionDtos.SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan, List<SubscriptionPlanFeature> features) {
        List<SubscriptionDtos.SubscriptionFeatureResponse> featureResponses = features.stream()
                .map(planFeature -> new SubscriptionDtos.SubscriptionFeatureResponse(
                        planFeature.getFeature().getCode(),
                        planFeature.getFeature().getName(),
                        planFeature.getFeature().getModuleCode(),
                        planFeature.getFeature().getDescription(),
                        planFeature.getIsEnabled(),
                        planFeature.getFeatureLimit()
                ))
                .toList();
        Set<String> featureCodes = featureResponses.stream()
                .filter(SubscriptionDtos.SubscriptionFeatureResponse::enabled)
                .map(SubscriptionDtos.SubscriptionFeatureResponse::code)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new SubscriptionDtos.SubscriptionPlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getBillingPeriod(),
                plan.getMaxOrganizations(),
                plan.getUnlimitedOrganizations(),
                plan.getIsActive(),
                featureCodes,
                featureResponses
        );
    }

    private List<PlatformAdminDtos.CountByLabel> summarizeUsersByRole(List<User> users) {
        return users.stream()
                .collect(Collectors.groupingBy(
                        user -> user.getRole() == null ? "UNASSIGNED" : user.getRole().getCode(),
                        LinkedHashMap::new,
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new PlatformAdminDtos.CountByLabel(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<PlatformAdminDtos.CountByLabel> summarizeStoresByStatus(List<Organization> organizations) {
        return organizations.stream()
                .collect(Collectors.groupingBy(
                        org -> Boolean.TRUE.equals(org.getIsActive()) ? "ACTIVE" : "INACTIVE",
                        LinkedHashMap::new,
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new PlatformAdminDtos.CountByLabel(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<PlatformAdminDtos.CountByLabel> summarizeSupportByStatus(List<PlatformAdminDtos.SupportItemSummaryResponse> supportItems) {
        return supportItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.status() == null || item.status().isBlank() ? "UNKNOWN" : item.status(),
                        LinkedHashMap::new,
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new PlatformAdminDtos.CountByLabel(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<PlatformAdminDtos.CountByLabel> summarizeFeedbackByVisitStatus(List<PlatformAdminDtos.FeedbackSummaryResponse> feedbackItems) {
        return feedbackItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.visitStatus() == null || item.visitStatus().isBlank() ? "UNKNOWN" : item.visitStatus(),
                        LinkedHashMap::new,
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new PlatformAdminDtos.CountByLabel(entry.getKey(), entry.getValue()))
                .toList();
    }

    private boolean isClosedStatus(String status) {
        if (status == null) {
            return false;
        }
        return switch (status.trim().toUpperCase()) {
            case "RESOLVED", "CLOSED", "COMPLETED", "CANCELLED", "REJECTED" -> true;
            default -> false;
        };
    }

    private PlatformAdminDtos.StoreSummaryResponse toStoreSummary(Organization organization) {
        AccountSubscription subscription = organization.getOwnerAccountId() == null
                ? null
                : accountSubscriptionRepository.findCurrentSubscription(organization.getOwnerAccountId(), LocalDate.now()).orElse(null);
        return new PlatformAdminDtos.StoreSummaryResponse(
                organization.getId(),
                organization.getCode(),
                organization.getName(),
                organization.getOwnerAccountId(),
                organization.getIsActive(),
                branchRepository.findByOrganizationIdOrderByIdAsc(organization.getId()).size(),
                warehouseRepository.findByOrganizationIdOrderByBranchIdAscIdAsc(organization.getId()).size(),
                userRepository.findByOrganizationIdOrderByIdAsc(organization.getId()).size(),
                subscription == null ? null : subscription.getPlan().getCode(),
                subscription == null ? null : subscription.getPlan().getName(),
                subscription == null ? "NONE" : subscription.getStatus(),
                organization.getSubscriptionVersion(),
                subscription == null ? null : subscription.getStartsOn(),
                subscription == null ? null : subscription.getEndsOn()
        );
    }

    private PlatformAdminDtos.SubscriptionSummaryResponse toSubscriptionSummary(Organization organization) {
        AccountSubscription subscription = organization.getOwnerAccountId() == null
                ? null
                : accountSubscriptionRepository.findCurrentSubscription(organization.getOwnerAccountId(), LocalDate.now()).orElse(null);
        SubscriptionPlan plan = subscription == null ? null : subscription.getPlan();
        return new PlatformAdminDtos.SubscriptionSummaryResponse(
                organization.getId(),
                organization.getCode(),
                organization.getName(),
                organization.getOwnerAccountId(),
                plan == null ? null : plan.getCode(),
                plan == null ? null : plan.getName(),
                subscription == null ? "NONE" : subscription.getStatus(),
                subscription == null ? null : subscription.getStartsOn(),
                subscription == null ? null : subscription.getEndsOn(),
                subscription == null ? null : subscription.getAutoRenew(),
                plan == null ? null : plan.getMaxOrganizations(),
                plan == null ? null : plan.getUnlimitedOrganizations(),
                organization.getOwnerAccountId() == null ? 0L : organizationRepository.countByOwnerAccountIdAndIsActiveTrue(organization.getOwnerAccountId())
        );
    }

    private void applyPlanChanges(SubscriptionPlan plan, PlatformAdminDtos.SubscriptionPlanUpsertRequest request) {
        plan.setCode(request.code().trim().toUpperCase());
        plan.setName(request.name().trim());
        plan.setDescription(trimToNull(request.description()));
        plan.setBillingPeriod(request.billingPeriod().trim().toUpperCase());
        plan.setMaxOrganizations(request.maxOrganizations());
        plan.setUnlimitedOrganizations(Boolean.TRUE.equals(request.unlimitedOrganizations()));
        if (Boolean.TRUE.equals(plan.getUnlimitedOrganizations())) {
            plan.setMaxOrganizations(null);
        }
        if (request.active() != null) {
            plan.setIsActive(request.active());
        }
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
    }

    private User requireUser(Long organizationId, Long userId) {
        return userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Role resolveRole(String roleCode) {
        return roleRepository.findByCode(roleCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleCode));
    }

    private void validateBranches(Long organizationId, List<Long> branchIds, Long defaultBranchId) {
        if (branchIds == null || branchIds.isEmpty()) {
            throw new BusinessException("At least one branch must be assigned");
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(branchIds);
        for (Long branchId : uniqueIds) {
            branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                    .filter(branch -> !Boolean.FALSE.equals(branch.getIsActive()))
                    .orElseThrow(() -> new BusinessException("Branch does not belong to organization or is inactive: " + branchId));
        }
        if (defaultBranchId != null && !uniqueIds.contains(defaultBranchId)) {
            throw new BusinessException("Default branch must be part of assigned branches");
        }
    }

    private void replaceBranchAccess(Long userId, List<Long> branchIds, Long defaultBranchId) {
        userBranchAccessRepository.deleteAll(userBranchAccessRepository.findByUserId(userId));
        for (Long branchId : new LinkedHashSet<>(branchIds)) {
            UserBranchAccess access = new UserBranchAccess();
            access.setUserId(userId);
            access.setBranchId(branchId);
            access.setIsDefault(defaultBranchId != null && defaultBranchId.equals(branchId));
            userBranchAccessRepository.save(access);
        }
    }

    private String resolveEmployeeCode(Long organizationId, Long userId, String requestedCode) {
        String normalized = trimToNull(requestedCode);
        if (normalized != null) {
            String code = normalized.toUpperCase();
            boolean exists = Boolean.TRUE.equals(userRepository.existsByOrganizationIdAndEmployeeCodeAndIdNot(organizationId, code, userId));
            if (exists) {
                throw new BusinessException("Employee code already exists: " + code);
            }
            return code;
        }
        User existing = requireUser(organizationId, userId);
        if (trimToNull(existing.getEmployeeCode()) != null) {
            return existing.getEmployeeCode().trim().toUpperCase();
        }
        Organization organization = requireOrganization(organizationId);
        String orgCode = organization.getCode().trim().toUpperCase();
        for (int sequence = 1; sequence < 100000; sequence++) {
            String generated = "EMP-" + orgCode + "-" + String.format("%04d", sequence);
            if (!Boolean.TRUE.equals(userRepository.existsByOrganizationIdAndEmployeeCode(organizationId, generated))) {
                return generated;
            }
        }
        throw new BusinessException("Unable to generate employee code for organization " + organizationId);
    }

    private void ensureOwnerMembership(Organization organization, Account ownerAccount) {
        Role ownerRole = roleRepository.findByCode("OWNER")
                .orElseThrow(() -> new ResourceNotFoundException("Owner role not found"));
        organizationPersonProfileRepository.findByOrganizationIdAndPersonId(organization.getId(), ownerAccount.getPerson().getId())
                .orElseGet(() -> organizationPersonProfileRepository.save(OrganizationPersonProfile.builder()
                        .organizationId(organization.getId())
                        .person(ownerAccount.getPerson())
                        .displayName(ownerAccount.getPerson().getLegalName())
                        .emailForOrg(ownerAccount.getPerson().getPrimaryEmail())
                        .phoneForOrg(ownerAccount.getPerson().getPrimaryPhone())
                        .active(true)
                        .build()));
        userRepository.findByLoginAndOrganizationId(ownerAccount.getLoginIdentifier(), organization.getId())
                .orElseGet(() -> {
                    User membership = new User();
                    membership.setOrganizationId(organization.getId());
                    membership.setPersonId(ownerAccount.getPerson().getId());
                    membership.setAccountId(ownerAccount.getId());
                    membership.setRole(ownerRole);
                    membership.setEmployeeCode(ownerAccount.getLoginIdentifier());
                    membership.setDefaultBranchId(null);
                    membership.setActive(true);
                    return userRepository.save(membership);
                });
    }

    private EmployeeManagementResponses.EmployeeResponse toEmployeeResponse(User user) {
        List<EmployeeManagementResponses.BranchAccessSummary> branchAccess = userBranchAccessRepository.findByUserId(user.getId()).stream()
                .map(access -> new EmployeeManagementResponses.BranchAccessSummary(access.getBranchId(), access.getIsDefault()))
                .toList();
        return new EmployeeManagementResponses.EmployeeResponse(
                user.getId(),
                user.getOrganizationId(),
                user.getAccountId(),
                user.getPersonId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole() == null ? null : user.getRole().getCode(),
                user.getRole() == null ? null : user.getRole().getName(),
                user.getEmployeeCode(),
                user.getDefaultBranchId(),
                branchAccess,
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private ErpServiceDtos.ServiceTicketResponse toTicketResponse(ServiceTicket ticket) {
        return new ErpServiceDtos.ServiceTicketResponse(
                ticket.getId(),
                ticket.getOrganizationId(),
                ticket.getBranchId(),
                ticket.getCustomerId(),
                ticket.getSalesInvoiceId(),
                ticket.getSalesReturnId(),
                ticket.getTicketNumber(),
                ticket.getSourceType(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getComplaintSummary(),
                ticket.getIssueDescription(),
                ticket.getReportedOn(),
                ticket.getAssignedToUserId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private ErpServiceDtos.WarrantyClaimResponse toClaimResponse(WarrantyClaim claim) {
        return new ErpServiceDtos.WarrantyClaimResponse(
                claim.getId(),
                claim.getOrganizationId(),
                claim.getBranchId(),
                claim.getServiceTicketId(),
                claim.getCustomerId(),
                claim.getProductId(),
                claim.getSerialNumberId(),
                claim.getProductOwnershipId(),
                claim.getSalesInvoiceId(),
                claim.getSalesReturnId(),
                claim.getSupplierId(),
                claim.getDistributorId(),
                claim.getUpstreamRouteType(),
                claim.getUpstreamCompanyName(),
                claim.getUpstreamReferenceNumber(),
                claim.getUpstreamStatus(),
                claim.getRoutedOn(),
                claim.getClaimNumber(),
                claim.getClaimType(),
                claim.getStatus(),
                claim.getClaimDate(),
                claim.getApprovedOn(),
                claim.getWarrantyStartDate(),
                claim.getWarrantyEndDate(),
                null,
                null,
                claim.getClaimNotes(),
                claim.getCreatedAt(),
                claim.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
