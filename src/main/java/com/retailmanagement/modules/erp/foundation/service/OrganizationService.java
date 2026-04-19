package com.retailmanagement.modules.erp.foundation.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.auth.model.Account;
import com.retailmanagement.modules.auth.model.OrganizationPersonProfile;
import com.retailmanagement.modules.auth.model.Person;
import com.retailmanagement.modules.auth.model.Role;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.AccountRepository;
import com.retailmanagement.modules.auth.repository.OrganizationPersonProfileRepository;
import com.retailmanagement.modules.auth.repository.PersonRepository;
import com.retailmanagement.modules.auth.repository.RoleRepository;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.auth.security.UserPrincipal;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.dto.OrganizationDtos;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import java.util.Locale;
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
 private final AccountRepository accountRepository;
 private final PersonRepository personRepository;
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
  String requestedCode = normalizeCode(organization.getCode());
  if (requestedCode == null) {
   requestedCode = generateOrganizationCode();
  } else if (repository.existsByCodeIgnoreCase(requestedCode)) {
   throw new BusinessException("Organization code already exists: " + requestedCode);
  }
  organization.setCode(requestedCode);
  if (organization.getName() != null) {
   organization.setName(organization.getName().trim());
  }
  if (organization.getLegalName() != null) {
   organization.setLegalName(organization.getLegalName().trim());
  }

  UserPrincipal principal = ErpSecurityUtils.requirePrincipal();
  Long ownerAccountId = ErpSecurityUtils.currentAccountId()
          .orElseThrow(() -> new BusinessException("Authenticated owner account context is missing"));

  User creatorMembership = principal.getId() == null ? null : userRepository.findById(principal.getId()).orElse(null);
  if (creatorMembership != null) {
   SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot = subscriptionAccessService.currentSnapshot(creatorMembership.getOrganizationId());
   if (!Boolean.TRUE.equals(subscriptionSnapshot.canCreateOrganization())) {
    String limitMessage = Boolean.TRUE.equals(subscriptionSnapshot.unlimitedOrganizations())
            ? "Owner subscription does not allow creating additional organizations"
            : "Owner subscription allows only " + subscriptionSnapshot.maxOrganizations() + " organizations";
    throw new BusinessException(limitMessage);
   }
  }

  organization.setOwnerAccountId(ownerAccountId);
  Organization savedOrganization = repository.save(organization);

  Role ownerRole = roleRepository.findByCode("OWNER")
          .orElseThrow(() -> new BusinessException("Owner role not found"));

  Account ownerAccount = accountRepository.findById(ownerAccountId)
          .orElseThrow(() -> new BusinessException("Authenticated owner account not found"));
  Long personId = principal.getPersonId() != null ? principal.getPersonId() : ownerAccount.getPerson().getId();
  Person person = personRepository.findById(personId)
          .orElseThrow(() -> new BusinessException("Authenticated person not found"));
  String displayName = creatorMembership != null && creatorMembership.getDisplayName() != null
          ? creatorMembership.getDisplayName()
          : (person.getLegalName() != null ? person.getLegalName() : principal.getUsername());
  String email = creatorMembership != null && creatorMembership.getEmail() != null
          ? creatorMembership.getEmail()
          : person.getPrimaryEmail();
  String phone = creatorMembership != null && creatorMembership.getPhone() != null
          ? creatorMembership.getPhone()
          : person.getPrimaryPhone();
  String employeeCode = creatorMembership != null && creatorMembership.getEmployeeCode() != null
          ? creatorMembership.getEmployeeCode()
          : principal.getUsername();

  organizationPersonProfileRepository.findByOrganizationIdAndPersonId(savedOrganization.getId(), personId)
          .orElseGet(() -> organizationPersonProfileRepository.save(OrganizationPersonProfile.builder()
                  .organizationId(savedOrganization.getId())
                  .person(person)
                  .displayName(displayName)
                  .emailForOrg(email)
                  .phoneForOrg(phone)
                  .active(true)
                  .build()));

  User membership = new User();
  membership.setOrganizationId(savedOrganization.getId());
  membership.setPersonId(personId);
  membership.setAccountId(ownerAccountId);
  membership.setRole(ownerRole);
  membership.setEmployeeCode(employeeCode);
  membership.setDefaultBranchId(null);
  membership.setActive(true);
  userRepository.save(membership);

  return savedOrganization;
 }

 public Organization update(Long id, OrganizationDtos.UpdateOrganizationRequest request) {
  Organization organization = get(id);
  if (request.name() != null) organization.setName(request.name());
  if (request.code() != null) {
   String requestedCode = normalizeCode(request.code());
   if (requestedCode == null) {
    throw new BusinessException("Organization code cannot be blank");
   }
   if (repository.existsByCodeIgnoreCaseAndIdNot(requestedCode, id)) {
    throw new BusinessException("Organization code already exists: " + requestedCode);
   }
   organization.setCode(requestedCode);
  }
  if (request.legalName() != null) organization.setLegalName(request.legalName());
  if (request.phone() != null) organization.setPhone(request.phone());
  if (request.email() != null) organization.setEmail(request.email());
  if (request.gstin() != null) organization.setGstin(request.gstin());
  if (request.gstThresholdAlertEnabled() != null) organization.setGstThresholdAlertEnabled(request.gstThresholdAlertEnabled());
  if (request.isActive() != null) organization.setIsActive(request.isActive());
  return repository.save(organization);
 }

 private String normalizeCode(String code) {
  if (code == null) {
   return null;
  }
  String normalized = code.trim();
  if (normalized.isEmpty()) {
   return null;
  }
  return normalized.toUpperCase(Locale.ROOT);
 }

 private String generateOrganizationCode() {
  long nextSeed = repository.findTopByOrderByIdDesc()
          .map(existing -> existing.getId() + 1)
          .orElse(1L);
  for (int offset = 0; offset < 1000; offset++) {
   String candidate = String.format("ORG%06d", nextSeed + offset);
   if (!repository.existsByCodeIgnoreCase(candidate)) {
    return candidate;
   }
  }
  throw new BusinessException("Unable to auto-generate organization code. Please retry.");
 }
}
