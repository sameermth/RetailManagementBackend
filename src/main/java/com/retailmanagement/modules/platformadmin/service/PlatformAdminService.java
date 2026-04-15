package com.retailmanagement.modules.platformadmin.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.auth.dto.response.EmployeeManagementResponses;
import com.retailmanagement.modules.auth.model.Account;
import com.retailmanagement.modules.auth.model.OrganizationPersonProfile;
import com.retailmanagement.modules.auth.model.Person;
import com.retailmanagement.modules.auth.model.Role;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.model.UserBranchAccess;
import com.retailmanagement.modules.auth.repository.AccountRepository;
import com.retailmanagement.modules.auth.repository.OrganizationPersonProfileRepository;
import com.retailmanagement.modules.auth.repository.PersonRepository;
import com.retailmanagement.modules.auth.repository.RoleRepository;
import com.retailmanagement.modules.auth.repository.UserBranchAccessRepository;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.erp.audit.entity.AuditEvent;
import com.retailmanagement.modules.erp.audit.repository.AuditEventRepository;
import com.retailmanagement.modules.erp.catalog.entity.Product;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.ProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.foundation.dto.BranchDtos;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
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
import com.retailmanagement.modules.platformadmin.entity.PlatformIncident;
import com.retailmanagement.modules.report.model.ReportSchedule;
import com.retailmanagement.modules.report.repository.ReportScheduleRepository;
import com.retailmanagement.modules.platformadmin.dto.PlatformAdminDtos;
import com.retailmanagement.modules.platformadmin.repository.PlatformIncidentRepository;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformAdminService {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StoreProductRepository storeProductRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
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
    private final PlatformIncidentRepository platformIncidentRepository;
    private final AuditEventRepository auditEventRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

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

    public List<PlatformAdminDtos.OwnerAccountReferenceResponse> ownerAccounts(String query) {
        String normalizedQuery = trimToNull(query);
        return accountRepository.findOwnerAccountsForPlatformAdmin().stream()
                .filter(account -> matchesOwnerAccountQuery(account, normalizedQuery))
                .sorted(Comparator
                        .comparing((Account account) -> account.getPerson() == null ? "" : Objects.toString(account.getPerson().getLegalName(), ""))
                        .thenComparing(Account::getLoginIdentifier, String.CASE_INSENSITIVE_ORDER))
                .map(this::toOwnerAccountReference)
                .toList();
    }

    public PlatformAdminDtos.StoreSummaryResponse getStore(Long organizationId) {
        return toStoreSummary(requireOrganization(organizationId));
    }

    @Transactional
    public PlatformAdminDtos.StoreSummaryResponse createStore(PlatformAdminDtos.StoreUpsertRequest request) {
        Account ownerAccount = accountRepository.findById(request.ownerAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner account not found: " + request.ownerAccountId()));
        Organization saved = createOrganization(
                request.name(),
                request.code(),
                request.legalName(),
                request.phone(),
                request.email(),
                request.gstin(),
                request.gstThresholdAmount(),
                request.gstThresholdAlertEnabled(),
                request.isActive(),
                ownerAccount
        );
        ensureOwnerMembership(saved, ownerAccount);
        return toStoreSummary(saved);
    }

    @Transactional
    public PlatformAdminDtos.StoreOnboardingResponse onboardStore(PlatformAdminDtos.StoreOnboardingRequest request) {
        Account ownerAccount = createOwnerAccount(request.owner());
        Organization organization = createOrganization(
                request.store().name(),
                request.store().code(),
                request.store().legalName(),
                request.store().phone(),
                request.store().email(),
                request.store().gstin(),
                request.store().gstThresholdAmount(),
                request.store().gstThresholdAlertEnabled(),
                request.store().isActive(),
                ownerAccount
        );
        User ownerMembership = ensureOwnerMembership(organization, ownerAccount);

        Branch defaultBranch = null;
        if (request.branch() != null) {
            defaultBranch = createBranch(organization.getId(), request.branch());
            ownerMembership.setDefaultBranchId(defaultBranch.getId());
            ownerMembership = userRepository.save(ownerMembership);
            replaceBranchAccess(ownerMembership.getId(), List.of(defaultBranch.getId()), defaultBranch.getId());
        }

        if (request.subscription() != null) {
            subscriptionManagementService.activateSubscription(organization.getId(), request.subscription());
        }

        return new PlatformAdminDtos.StoreOnboardingResponse(
                toStoreSummary(organization),
                toOwnerAccountReference(ownerAccount),
                defaultBranch == null ? null : toBranchResponse(defaultBranch),
                toSubscriptionSummary(organization)
        );
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

    public List<PlatformAdminDtos.CatalogProductGovernanceResponse> catalogProducts(String query, String governanceStatus) {
        String normalizedQuery = trimToNull(query);
        String normalizedGovernanceStatus = trimToNull(governanceStatus);
        return productRepository.findAll().stream()
                .filter(product -> matchesCatalogProductQuery(product, normalizedQuery))
                .filter(product -> normalizedGovernanceStatus == null
                        || normalizedGovernanceStatus.equalsIgnoreCase(defaultProductGovernanceStatus(product)))
                .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toCatalogProductGovernance)
                .toList();
    }

    public PlatformAdminDtos.CatalogProductImpactResponse catalogProductImpact(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        List<StoreProduct> linkedStores = storeProductRepository.findByProductId(productId).stream()
                .sorted(Comparator
                        .comparing(StoreProduct::getOrganizationId)
                        .thenComparing(StoreProduct::getSku, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        Map<Long, Organization> organizations = organizationRepository.findAllById(
                        linkedStores.stream().map(StoreProduct::getOrganizationId).filter(Objects::nonNull).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(Organization::getId, org -> org));
        return new PlatformAdminDtos.CatalogProductImpactResponse(
                product.getId(),
                product.getName(),
                defaultProductGovernanceStatus(product),
                Boolean.TRUE.equals(product.getBlockNewStoreAdoption()),
                Boolean.TRUE.equals(product.getBlockTransactions()),
                linkedStores.size(),
                linkedStores.stream()
                        .map(storeProduct -> {
                            Organization organization = organizations.get(storeProduct.getOrganizationId());
                            return new PlatformAdminDtos.CatalogProductImpactStoreResponse(
                                    storeProduct.getId(),
                                    storeProduct.getOrganizationId(),
                                    organization == null ? null : organization.getCode(),
                                    organization == null ? null : organization.getName(),
                                    storeProduct.getSku(),
                                    storeProduct.getName(),
                                    storeProduct.getIsActive()
                            );
                        })
                        .toList()
        );
    }

    @Transactional
    public PlatformAdminDtos.CatalogProductGovernanceResponse updateProductGovernance(
            Long productId,
            PlatformAdminDtos.UpdateProductGovernanceRequest request
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        applyGovernanceValues(
                product,
                request.governanceStatus(),
                request.qualityReviewStatus(),
                request.blockNewStoreAdoption(),
                request.blockTransactions(),
                request.governanceReason()
        );
        return toCatalogProductGovernance(productRepository.save(product));
    }

    public List<PlatformAdminDtos.PlatformIncidentResponse> incidents(String status, String subjectType, Long organizationId) {
        String normalizedStatus = trimToNull(status);
        String normalizedSubjectType = trimToNull(subjectType);
        return platformIncidentRepository.findAllByOrderByOpenedAtDescIdDesc().stream()
                .filter(incident -> organizationId == null || Objects.equals(organizationId, incident.getOrganizationId()))
                .filter(incident -> normalizedStatus == null || normalizedStatus.equalsIgnoreCase(incident.getStatus()))
                .filter(incident -> normalizedSubjectType == null || normalizedSubjectType.equalsIgnoreCase(incident.getSubjectType()))
                .map(this::toPlatformIncidentResponse)
                .toList();
    }

    @Transactional
    public PlatformAdminDtos.PlatformIncidentResponse createIncident(PlatformAdminDtos.CreatePlatformIncidentRequest request) {
        validateIncidentReferences(request);
        PlatformIncident incident = new PlatformIncident();
        incident.setIncidentNumber(generateIncidentNumber());
        incident.setOrganizationId(request.organizationId());
        incident.setSubjectType(request.subjectType().trim().toUpperCase());
        incident.setIncidentType(request.incidentType().trim().toUpperCase());
        incident.setSeverity(request.severity().trim().toUpperCase());
        incident.setStatus("OPEN");
        incident.setTitle(request.title().trim());
        incident.setDescription(trimToNull(request.description()));
        incident.setProductId(request.productId());
        incident.setStoreProductId(request.storeProductId());
        incident.setServiceTicketId(request.serviceTicketId());
        incident.setWarrantyClaimId(request.warrantyClaimId());
        incident.setReportedBy(trimToNull(request.reportedBy()));
        incident.setRecommendedAction(trimToNull(request.recommendedAction()));
        incident.setOpenedAt(LocalDateTime.now());
        return toPlatformIncidentResponse(platformIncidentRepository.save(incident));
    }

    @Transactional
    public PlatformAdminDtos.PlatformIncidentResponse updateIncidentStatus(
            Long incidentId,
            PlatformAdminDtos.UpdatePlatformIncidentStatusRequest request
    ) {
        PlatformIncident incident = platformIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Platform incident not found: " + incidentId));
        incident.setStatus(request.status().trim().toUpperCase());
        incident.setActionTaken(trimToNull(request.actionTaken()));
        incident.setResolutionNotes(trimToNull(request.resolutionNotes()));
        if (isClosedStatus(incident.getStatus())) {
            incident.setResolvedAt(LocalDateTime.now());
        } else {
            incident.setResolvedAt(null);
        }
        return toPlatformIncidentResponse(platformIncidentRepository.save(incident));
    }

    @Transactional
    public PlatformAdminDtos.IncidentGovernanceActionResponse applyIncidentGovernanceAction(
            Long incidentId,
            PlatformAdminDtos.ApplyIncidentGovernanceActionRequest request
    ) {
        PlatformIncident incident = platformIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Platform incident not found: " + incidentId));
        if (incident.getProductId() == null) {
            throw new BusinessException("Platform incident is not linked to a catalog product");
        }
        Long productId = incident.getProductId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        GovernancePreset preset = governancePreset(request.actionType());
        String governanceReason = trimToNull(request.governanceReason());
        if (governanceReason == null) {
            governanceReason = incident.getTitle();
        }
        applyGovernanceValues(
                product,
                preset.governanceStatus(),
                preset.qualityReviewStatus(),
                preset.blockNewStoreAdoption(),
                preset.blockTransactions(),
                governanceReason
        );
        product = productRepository.save(product);

        incident.setActionTaken(actionLabel(request.actionType()));
        incident.setResolutionNotes(trimToNull(request.resolutionNotes()));
        incident.setStatus(defaultIfBlank(request.incidentStatus(), "ACTIONED"));
        if (isClosedStatus(incident.getStatus())) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        incident = platformIncidentRepository.save(incident);

        return new PlatformAdminDtos.IncidentGovernanceActionResponse(
                toPlatformIncidentResponse(incident),
                toCatalogProductGovernance(product)
        );
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
        return jdbcTemplate.query("""
                        SELECT id,
                               organization_id,
                               user_id,
                               channel,
                               status,
                               reference_type,
                               reference_id,
                               scheduled_at,
                               sent_at,
                               read_at,
                               created_at
                        FROM notification
                        ORDER BY created_at DESC, id DESC
                        LIMIT 200
                        """,
                (rs, rowNum) -> new PlatformAdminDtos.NotificationSummaryResponse(
                        rs.getLong("id"),
                        "NTF-" + rs.getLong("id"),
                        rs.getString("reference_type"),
                        rs.getString("channel"),
                        rs.getString("status"),
                        null,
                        null,
                        null,
                        rs.getString("reference_type"),
                        getNullableLong(rs, "reference_id"),
                        toLocalDateTime(rs.getTimestamp("scheduled_at")),
                        toLocalDateTime(rs.getTimestamp("sent_at")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        null,
                        null
                ));
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
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        List<PlatformAdminDtos.CountByLabel> notificationsByStatus = jdbcTemplate.query("""
                        SELECT status, COUNT(*) AS item_count
                        FROM notification
                        GROUP BY status
                        ORDER BY status
                        """,
                (rs, rowNum) -> new PlatformAdminDtos.CountByLabel(rs.getString("status"), rs.getLong("item_count")));
        List<PlatformAdminDtos.CountByLabel> notificationsByType = jdbcTemplate.query("""
                        SELECT COALESCE(reference_type, 'UNKNOWN') AS type_label, COUNT(*) AS item_count
                        FROM notification
                        GROUP BY COALESCE(reference_type, 'UNKNOWN')
                        ORDER BY type_label
                        """,
                (rs, rowNum) -> new PlatformAdminDtos.CountByLabel(rs.getString("type_label"), rs.getLong("item_count")));
        Long totalNotifications = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification", Long.class);
        Long pendingNotifications = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification WHERE status = 'PENDING'", Long.class);
        Long failedNotifications = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification WHERE status = 'FAILED'", Long.class);
        Long unreadNotifications = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification WHERE read_at IS NULL", Long.class);
        return new PlatformAdminDtos.SystemHealthResponse(
                totalNotifications == null ? 0 : totalNotifications,
                pendingNotifications == null ? 0 : pendingNotifications,
                failedNotifications == null ? 0 : failedNotifications,
                unreadNotifications == null ? 0 : unreadNotifications,
                reportScheduleRepository.findByIsActiveTrue().size(),
                auditEvents.size(),
                notificationsByStatus,
                notificationsByType
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

    private boolean matchesCatalogProductQuery(Product product, String query) {
        if (query == null) {
            return true;
        }
        String normalized = query.toLowerCase();
        return containsIgnoreCase(product.getName(), normalized)
                || containsIgnoreCase(product.getBrandName(), normalized)
                || containsIgnoreCase(product.getCategoryName(), normalized)
                || containsIgnoreCase(product.getHsnCode(), normalized);
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

    private PlatformAdminDtos.OwnerAccountReferenceResponse toOwnerAccountReference(Account account) {
        return new PlatformAdminDtos.OwnerAccountReferenceResponse(
                account.getId(),
                account.getLoginIdentifier(),
                account.getPerson() == null ? null : account.getPerson().getLegalName(),
                account.getPerson() == null ? null : account.getPerson().getPrimaryEmail(),
                account.getPerson() == null ? null : account.getPerson().getPrimaryPhone(),
                account.getActive()
        );
    }

    private PlatformAdminDtos.CatalogProductGovernanceResponse toCatalogProductGovernance(Product product) {
        return new PlatformAdminDtos.CatalogProductGovernanceResponse(
                product.getId(),
                product.getName(),
                product.getBrandName(),
                product.getCategoryName(),
                product.getHsnCode(),
                product.getIsServiceItem(),
                product.getIsActive(),
                defaultProductGovernanceStatus(product),
                product.getQualityReviewStatus() == null ? "NORMAL" : product.getQualityReviewStatus(),
                Boolean.TRUE.equals(product.getBlockNewStoreAdoption()),
                Boolean.TRUE.equals(product.getBlockTransactions()),
                product.getGovernanceReason(),
                product.getGovernanceUpdatedAt(),
                platformIncidentRepository.countByProductId(product.getId())
        );
    }

    private String defaultProductGovernanceStatus(Product product) {
        return product.getGovernanceStatus() == null ? "ACTIVE" : product.getGovernanceStatus();
    }

    private PlatformAdminDtos.PlatformIncidentResponse toPlatformIncidentResponse(PlatformIncident incident) {
        return new PlatformAdminDtos.PlatformIncidentResponse(
                incident.getId(),
                incident.getIncidentNumber(),
                incident.getOrganizationId(),
                incident.getSubjectType(),
                incident.getIncidentType(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getProductId(),
                incident.getStoreProductId(),
                incident.getServiceTicketId(),
                incident.getWarrantyClaimId(),
                incident.getReportedBy(),
                incident.getRecommendedAction(),
                incident.getActionTaken(),
                incident.getResolutionNotes(),
                incident.getOpenedAt(),
                incident.getResolvedAt(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }

    private BranchDtos.BranchResponse toBranchResponse(Branch branch) {
        return new BranchDtos.BranchResponse(
                branch.getId(),
                branch.getOrganizationId(),
                branch.getCode(),
                branch.getName(),
                branch.getPhone(),
                branch.getEmail(),
                branch.getAddressLine1(),
                branch.getAddressLine2(),
                branch.getCity(),
                branch.getState(),
                branch.getPostalCode(),
                branch.getCountry(),
                branch.getIsActive(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }

    private boolean matchesOwnerAccountQuery(Account account, String query) {
        if (query == null) {
            return true;
        }
        String normalized = query.toLowerCase();
        return containsIgnoreCase(account.getLoginIdentifier(), normalized)
                || containsIgnoreCase(account.getPerson() == null ? null : account.getPerson().getLegalName(), normalized)
                || containsIgnoreCase(account.getPerson() == null ? null : account.getPerson().getPrimaryEmail(), normalized)
                || containsIgnoreCase(account.getPerson() == null ? null : account.getPerson().getPrimaryPhone(), normalized);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private Long getNullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
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

    private void validateIncidentReferences(PlatformAdminDtos.CreatePlatformIncidentRequest request) {
        if (request.productId() == null
                && request.storeProductId() == null
                && request.serviceTicketId() == null
                && request.warrantyClaimId() == null) {
            throw new BusinessException("Incident must reference a product, store product, service ticket, or warranty claim");
        }
        if (request.productId() != null) {
            productRepository.findById(request.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
        }
        if (request.storeProductId() != null) {
            StoreProduct storeProduct = storeProductRepository.findById(request.storeProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Store product not found: " + request.storeProductId()));
            if (request.organizationId() != null && !request.organizationId().equals(storeProduct.getOrganizationId())) {
                throw new BusinessException("Store product does not belong to the selected organization");
            }
        }
        if (request.serviceTicketId() != null) {
            ServiceTicket ticket = serviceTicketRepository.findById(request.serviceTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service ticket not found: " + request.serviceTicketId()));
            if (request.organizationId() != null && !request.organizationId().equals(ticket.getOrganizationId())) {
                throw new BusinessException("Service ticket does not belong to the selected organization");
            }
        }
        if (request.warrantyClaimId() != null) {
            WarrantyClaim claim = warrantyClaimRepository.findById(request.warrantyClaimId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found: " + request.warrantyClaimId()));
            if (request.organizationId() != null && !request.organizationId().equals(claim.getOrganizationId())) {
                throw new BusinessException("Warranty claim does not belong to the selected organization");
            }
        }
    }

    private void applyGovernanceValues(
            Product product,
            String governanceStatus,
            String qualityReviewStatus,
            Boolean blockNewStoreAdoption,
            Boolean blockTransactions,
            String governanceReason
    ) {
        product.setGovernanceStatus(governanceStatus.trim().toUpperCase());
        product.setQualityReviewStatus(qualityReviewStatus.trim().toUpperCase());
        product.setBlockNewStoreAdoption(Boolean.TRUE.equals(blockNewStoreAdoption));
        product.setBlockTransactions(Boolean.TRUE.equals(blockTransactions));
        product.setGovernanceReason(trimToNull(governanceReason));
        product.setGovernanceUpdatedAt(LocalDateTime.now());
    }

    private GovernancePreset governancePreset(String actionType) {
        String normalizedAction = actionType.trim().toUpperCase();
        return switch (normalizedAction) {
            case "RESTRICT_PRODUCT" -> new GovernancePreset("RESTRICTED", "UNDER_REVIEW", true, false);
            case "DISCONTINUE_PRODUCT" -> new GovernancePreset("DISCONTINUED", "UNDER_REVIEW", true, false);
            case "BLOCK_PRODUCT" -> new GovernancePreset("BLOCKED", "FAILED", true, true);
            case "RECALL_PRODUCT" -> new GovernancePreset("RECALLED", "FAILED", true, true);
            case "CLEAR_PRODUCT_RESTRICTIONS" -> new GovernancePreset("ACTIVE", "NORMAL", false, false);
            default -> throw new BusinessException("Unsupported governance action: " + actionType);
        };
    }

    private String actionLabel(String actionType) {
        return actionType.trim().toUpperCase().replace('_', ' ');
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized.toUpperCase();
    }

    private String generateIncidentNumber() {
        return "INC-" + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
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

    private Organization createOrganization(
            String name,
            String codeValue,
            String legalName,
            String phone,
            String email,
            String gstin,
            java.math.BigDecimal gstThresholdAmount,
            Boolean gstThresholdAlertEnabled,
            Boolean isActive,
            Account ownerAccount
    ) {
        String code = codeValue.trim().toUpperCase();
        if (organizationRepository.findByCode(code).isPresent()) {
            throw new BusinessException("Organization code already exists: " + code);
        }
        Organization organization = new Organization();
        organization.setName(name.trim());
        organization.setCode(code);
        organization.setLegalName(trimToNull(legalName));
        organization.setPhone(trimToNull(phone));
        organization.setEmail(trimToNull(email));
        organization.setGstin(trimToNull(gstin));
        organization.setOwnerAccountId(ownerAccount.getId());
        if (gstThresholdAmount != null) {
            organization.setGstThresholdAmount(gstThresholdAmount);
        }
        if (gstThresholdAlertEnabled != null) {
            organization.setGstThresholdAlertEnabled(gstThresholdAlertEnabled);
        }
        if (isActive != null) {
            organization.setIsActive(isActive);
        }
        return organizationRepository.save(organization);
    }

    private Account createOwnerAccount(PlatformAdminDtos.OwnerAccountCreateRequest request) {
        String loginIdentifier = request.loginIdentifier().trim();
        if (accountRepository.findByLoginIdentifierIgnoreCase(loginIdentifier).isPresent()) {
            throw new BusinessException("Login identifier already exists: " + loginIdentifier);
        }

        String email = trimToNull(request.email());
        if (email != null && personRepository.findFirstByPrimaryEmailIgnoreCase(email).isPresent()) {
            throw new BusinessException("Owner email already exists: " + email);
        }

        String phone = trimToNull(request.phone());
        if (phone != null && personRepository.findFirstByPrimaryPhone(phone).isPresent()) {
            throw new BusinessException("Owner phone already exists: " + phone);
        }

        Person person = Person.builder()
                .legalName(request.fullName().trim())
                .primaryEmail(email)
                .primaryPhone(phone)
                .status("ACTIVE")
                .build();
        person = personRepository.save(person);

        Account account = Account.builder()
                .person(person)
                .loginIdentifier(loginIdentifier)
                .passwordHash(passwordEncoder.encode(request.password()))
                .active(request.active() == null || request.active())
                .locked(false)
                .build();
        return accountRepository.save(account);
    }

    private Branch createBranch(Long organizationId, PlatformAdminDtos.StoreBranchSeedRequest request) {
        Branch branch = new Branch();
        branch.setOrganizationId(organizationId);
        branch.setCode(request.code().trim().toUpperCase());
        branch.setName(request.name().trim());
        branch.setPhone(trimToNull(request.phone()));
        branch.setEmail(trimToNull(request.email()));
        branch.setAddressLine1(trimToNull(request.addressLine1()));
        branch.setAddressLine2(trimToNull(request.addressLine2()));
        branch.setCity(trimToNull(request.city()));
        branch.setState(trimToNull(request.state()));
        branch.setPostalCode(trimToNull(request.postalCode()));
        branch.setCountry(trimToNull(request.country()));
        if (request.isActive() != null) {
            branch.setIsActive(request.isActive());
        }
        return branchRepository.save(branch);
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

    private User ensureOwnerMembership(Organization organization, Account ownerAccount) {
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
        return userRepository.findByLoginAndOrganizationId(ownerAccount.getLoginIdentifier(), organization.getId())
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

    private record GovernancePreset(
            String governanceStatus,
            String qualityReviewStatus,
            Boolean blockNewStoreAdoption,
            Boolean blockTransactions
    ) {}
}
