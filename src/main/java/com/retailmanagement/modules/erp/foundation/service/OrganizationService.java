package com.retailmanagement.modules.erp.foundation.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.auth.model.OrganizationPersonProfile;
import com.retailmanagement.modules.auth.model.Role;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.OrganizationPersonProfileRepository;
import com.retailmanagement.modules.auth.repository.RoleRepository;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.dto.OrganizationDtos;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class OrganizationService {
 private final OrganizationRepository repository;
 private final ErpAccessGuard accessGuard;
 private final UserRepository userRepository;
 private final RoleRepository roleRepository;
 private final OrganizationPersonProfileRepository organizationPersonProfileRepository;
 private final SubscriptionAccessService subscriptionAccessService;
 @Transactional(readOnly=true)
 public List<Organization> list(){
  String username = ErpSecurityUtils.currentUsername()
          .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Authenticated user context is missing"));
  List<Long> organizationIds = userRepository.findAllByLogin(username).stream()
          .filter(user -> Boolean.TRUE.equals(user.getActive()))
          .map(User::getOrganizationId)
          .distinct()
          .collect(Collectors.toList());
  return organizationIds.isEmpty() ? List.of() : repository.findByIdIn(organizationIds);
 }
 @Transactional(readOnly=true)
 public Organization get(Long id){
  accessGuard.assertOrganizationAccess(id);
  return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ERP organization not found: "+id));
 }
 public Organization create(Organization organization){
  User creator = userRepository.findById(ErpSecurityUtils.requirePrincipal().getId())
          .orElseThrow(() -> new BusinessException("Authenticated user not found"));
  Long ownerAccountId = ErpSecurityUtils.currentAccountId()
          .orElseThrow(() -> new BusinessException("Authenticated owner account context is missing"));
  SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot = subscriptionAccessService.currentSnapshot(creator.getOrganizationId());
  if (!Boolean.TRUE.equals(subscriptionSnapshot.canCreateOrganization())) {
   String limitMessage = Boolean.TRUE.equals(subscriptionSnapshot.unlimitedOrganizations())
           ? "Owner subscription does not allow creating additional organizations"
           : "Owner subscription allows only " + subscriptionSnapshot.maxOrganizations() + " organizations";
   throw new BusinessException(limitMessage);
  }

  organization.setOwnerAccountId(ownerAccountId);
  Organization savedOrganization = repository.save(organization);

  Role ownerRole = roleRepository.findByCode("OWNER")
          .orElseThrow(() -> new BusinessException("Owner role not found"));

  organizationPersonProfileRepository.findByOrganizationIdAndPersonId(savedOrganization.getId(), creator.getPersonId())
          .orElseGet(() -> organizationPersonProfileRepository.save(OrganizationPersonProfile.builder()
                  .organizationId(savedOrganization.getId())
                  .person(creator.getPerson())
                  .displayName(creator.getDisplayName())
                  .emailForOrg(creator.getEmail())
                  .phoneForOrg(creator.getPhone())
                  .active(true)
                  .build()));

  User membership = new User();
  membership.setOrganizationId(savedOrganization.getId());
  membership.setPersonId(creator.getPersonId());
  membership.setAccountId(ownerAccountId);
  membership.setRole(ownerRole);
  membership.setEmployeeCode(creator.getEmployeeCode());
  membership.setDefaultBranchId(null);
  membership.setActive(true);
  userRepository.save(membership);

  return savedOrganization;
 }

 public Organization update(Long id, OrganizationDtos.UpdateOrganizationRequest request) {
  Organization organization = get(id);
  if (request.name() != null) organization.setName(request.name());
  if (request.code() != null) organization.setCode(request.code());
  if (request.legalName() != null) organization.setLegalName(request.legalName());
  if (request.phone() != null) organization.setPhone(request.phone());
  if (request.email() != null) organization.setEmail(request.email());
  if (request.gstin() != null) organization.setGstin(request.gstin());
  if (request.gstThresholdAmount() != null) organization.setGstThresholdAmount(request.gstThresholdAmount());
  if (request.gstThresholdAlertEnabled() != null) organization.setGstThresholdAlertEnabled(request.gstThresholdAlertEnabled());
  if (request.isActive() != null) organization.setIsActive(request.isActive());
  return repository.save(organization);
 }
}
