package com.retailmanagement.modules.erp.common.security;

import com.retailmanagement.modules.auth.security.UserPrincipal;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ErpAccessGuard {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;

    public Long assertOrganizationAccess(Long organizationId) {
        UserPrincipal principal = ErpSecurityUtils.requirePrincipal();
        Long principalOrganizationId = principal.getOrganizationId();
        if (principalOrganizationId == null) {
            throw new AccessDeniedException("Authenticated organization context is missing");
        }
        if (!principalOrganizationId.equals(organizationId)) {
            throw new AccessDeniedException("Access denied for organization " + organizationId);
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new AccessDeniedException("Organization not found: " + organizationId));
        if (Boolean.FALSE.equals(organization.getIsActive())) {
            throw new AccessDeniedException("Organization is inactive: " + organizationId);
        }
        return organizationId;
    }

    public Long assertBranchAccess(Long organizationId, Long branchId) {
        assertOrganizationAccess(organizationId);
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new AccessDeniedException("Branch not found: " + branchId));
        if (!organizationId.equals(branch.getOrganizationId())) {
            throw new AccessDeniedException("Branch does not belong to organization " + organizationId);
        }
        if (Boolean.FALSE.equals(branch.getIsActive())) {
            throw new AccessDeniedException("Branch is inactive: " + branchId);
        }

        UserPrincipal principal = ErpSecurityUtils.requirePrincipal();
        if (principal.hasRole("OWNER")) {
            return branchId;
        }
        if (principal.hasBranchAccess(branchId)) {
            return branchId;
        }
        throw new AccessDeniedException("Access denied for branch " + branchId);
    }
}
