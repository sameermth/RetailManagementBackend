package com.retailmanagement.modules.auth.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.auth.dto.request.EmployeeManagementRequests;
import com.retailmanagement.modules.auth.dto.response.EmployeeManagementResponses;
import com.retailmanagement.modules.auth.model.OrganizationPersonProfile;
import com.retailmanagement.modules.auth.model.Person;
import com.retailmanagement.modules.auth.model.Role;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.model.UserBranchAccess;
import com.retailmanagement.modules.auth.model.Account;
import com.retailmanagement.modules.auth.repository.OrganizationPersonProfileRepository;
import com.retailmanagement.modules.auth.repository.PersonRepository;
import com.retailmanagement.modules.auth.repository.RoleRepository;
import com.retailmanagement.modules.auth.repository.UserBranchAccessRepository;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.auth.repository.AccountRepository;
import com.retailmanagement.modules.auth.service.EmployeeManagementService;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeManagementServiceImpl implements EmployeeManagementService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final OrganizationRepository organizationRepository;
    private final UserBranchAccessRepository userBranchAccessRepository;
    private final OrganizationPersonProfileRepository organizationPersonProfileRepository;
    private final ErpAccessGuard accessGuard;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeManagementResponses.EmployeeResponse> list(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        return userRepository.findByOrganizationIdOrderByIdAsc(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeManagementResponses.EmployeeResponse get(Long organizationId, Long userId) {
        accessGuard.assertOrganizationAccess(organizationId);
        return toResponse(requireUser(organizationId, userId));
    }

    @Override
    public EmployeeManagementResponses.EmployeeResponse create(EmployeeManagementRequests.CreateEmployeeRequest request) {
        accessGuard.assertOrganizationAccess(request.organizationId());
        if (accountRepository.findByLoginIdentifierIgnoreCase(request.username().trim()).isPresent()) {
            throw new BusinessException("Username already exists");
        }
        validateBranches(request.organizationId(), request.branchIds(), request.defaultBranchId());

        Person person = Person.builder()
                .legalName(request.fullName().trim())
                .primaryEmail(request.email())
                .primaryPhone(request.phone())
                .status("ACTIVE")
                .build();
        person = personRepository.save(person);

        Account account = Account.builder()
                .person(person)
                .loginIdentifier(request.username().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .active(Boolean.TRUE.equals(request.active()) || request.active() == null)
                .locked(false)
                .build();
        account = accountRepository.save(account);

        OrganizationPersonProfile organizationPersonProfile = organizationPersonProfileRepository.save(OrganizationPersonProfile.builder()
                .organizationId(request.organizationId())
                .person(person)
                .displayName(request.fullName().trim())
                .emailForOrg(request.email())
                .phoneForOrg(request.phone())
                .active(Boolean.TRUE.equals(request.active()) || request.active() == null)
                .build());

        Role role = resolveRole(request.roleCode());
        User user = new User();
        user.setOrganizationId(request.organizationId());
        user.setPersonId(person.getId());
        user.setAccountId(account.getId());
        user.setPerson(person);
        user.setAccount(account);
        user.setOrganizationPersonProfile(organizationPersonProfile);
        user.setRole(role);
        user.setEmployeeCode(resolveEmployeeCode(request.organizationId(), null, request.employeeCode()));
        user.setDefaultBranchId(request.defaultBranchId());
        user.setActive(Boolean.TRUE.equals(request.active()) || request.active() == null);
        User saved = userRepository.save(user);
        replaceBranchAccess(saved.getId(), request.branchIds(), request.defaultBranchId());
        return toResponse(requireUser(request.organizationId(), saved.getId()));
    }

    @Override
    public EmployeeManagementResponses.EmployeeResponse update(Long organizationId, Long userId, EmployeeManagementRequests.UpdateEmployeeRequest request) {
        accessGuard.assertOrganizationAccess(organizationId);
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
            if (user.getAccount() != null) user.getAccount().setActive(request.active());
        }
        userRepository.save(user);
        return toResponse(requireUser(organizationId, userId));
    }

    @Override
    public void activate(Long organizationId, Long userId) {
        User user = requireUser(organizationId, userId);
        user.setActive(true);
        if (user.getAccount() != null) user.getAccount().setActive(true);
        userRepository.save(user);
    }

    @Override
    public void deactivate(Long organizationId, Long userId) {
        User user = requireUser(organizationId, userId);
        user.setActive(false);
        if (user.getAccount() != null) user.getAccount().setActive(false);
        userRepository.save(user);
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
            Branch branch = branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                    .orElseThrow(() -> new BusinessException("Branch does not belong to organization: " + branchId));
            if (Boolean.FALSE.equals(branch.getIsActive())) {
                throw new BusinessException("Branch is inactive: " + branchId);
            }
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
            boolean exists = userId == null
                    ? Boolean.TRUE.equals(userRepository.existsByOrganizationIdAndEmployeeCode(organizationId, code))
                    : Boolean.TRUE.equals(userRepository.existsByOrganizationIdAndEmployeeCodeAndIdNot(organizationId, code, userId));
            if (exists) {
                throw new BusinessException("Employee code already exists: " + code);
            }
            return code;
        }
        if (userId != null) {
            User existing = requireUser(organizationId, userId);
            if (trimToNull(existing.getEmployeeCode()) != null) {
                return existing.getEmployeeCode().trim().toUpperCase();
            }
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        String orgCode = organization.getCode().trim().toUpperCase();
        for (int sequence = 1; sequence < 100000; sequence++) {
            String generated = "EMP-" + orgCode + "-" + String.format("%04d", sequence);
            if (!Boolean.TRUE.equals(userRepository.existsByOrganizationIdAndEmployeeCode(organizationId, generated))) {
                return generated;
            }
        }
        throw new BusinessException("Unable to generate employee code for organization " + organizationId);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private EmployeeManagementResponses.EmployeeResponse toResponse(User user) {
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
}
