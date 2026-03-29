package com.retailmanagement.modules.auth.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.auth.dto.request.ChangePasswordPayload;
import com.retailmanagement.modules.auth.dto.request.UpdateProfileRequest;
import com.retailmanagement.modules.auth.dto.response.ProfileResponse;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.AccountRepository;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.auth.service.ProfileService;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse currentProfile() {
        User user = currentUser();
        Organization organization = organizationRepository.findById(user.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + user.getOrganizationId()));
        return toResponse(user, organization);
    }

    @Override
    public ProfileResponse updateCurrentProfile(UpdateProfileRequest request) {
        User user = currentUser();
        if (user.getPerson() != null) {
            user.getPerson().setLegalName(request.getFullName().trim());
            user.getPerson().setPrimaryEmail(request.getEmail());
            user.getPerson().setPrimaryPhone(request.getPhone());
        }
        if (user.getOrganizationPersonProfile() != null) {
            user.getOrganizationPersonProfile().setDisplayName(request.getFullName().trim());
            user.getOrganizationPersonProfile().setEmailForOrg(request.getEmail());
            user.getOrganizationPersonProfile().setPhoneForOrg(request.getPhone());
        }
        User saved = userRepository.save(user);
        Organization organization = organizationRepository.findById(saved.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + saved.getOrganizationId()));
        return toResponse(saved, organization);
    }

    @Override
    public void changeCurrentPassword(ChangePasswordPayload request) {
        User user = currentUser();
        if (user.getAccount() == null) {
            throw new BusinessException("No account attached to current user");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getAccount().getPasswordHash())) {
            throw new BusinessException("Old password is incorrect");
        }
        user.getAccount().setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(user.getAccount());
    }

    private User currentUser() {
        Long userId = ErpSecurityUtils.currentUserId()
                .orElseThrow(() -> new BusinessException("Authenticated user context not found"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private ProfileResponse toResponse(User user, Organization organization) {
        return ProfileResponse.builder()
                .userId(user.getId())
                .accountId(user.getAccountId())
                .organizationId(user.getOrganizationId())
                .organizationCode(organization.getCode())
                .organizationName(organization.getName())
                .username(user.getUsername())
                .fullName(user.getDisplayName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(role -> "ROLE_" + role.getCode()).collect(Collectors.toCollection(LinkedHashSet::new)))
                .permissions(user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(permission -> permission.getCode())
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();
    }
}
