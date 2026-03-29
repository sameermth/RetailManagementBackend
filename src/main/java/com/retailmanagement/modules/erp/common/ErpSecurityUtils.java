package com.retailmanagement.modules.erp.common;

import com.retailmanagement.modules.auth.security.UserPrincipal;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class ErpSecurityUtils {
    private ErpSecurityUtils() {}

    public static Optional<UserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) return Optional.empty();
        if (authentication.getPrincipal() instanceof UserPrincipal principal) return Optional.of(principal);
        return Optional.empty();
    }

    public static UserPrincipal requirePrincipal() {
        return currentPrincipal()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Authenticated ERP user context not found"));
    }

    public static Optional<Long> currentUserId() {
        return currentPrincipal().map(UserPrincipal::getId);
    }

    public static Optional<String> currentUsername() {
        return currentPrincipal().map(UserPrincipal::getUsername);
    }

    public static Optional<Long> currentOrganizationId() {
        return currentPrincipal().map(UserPrincipal::getOrganizationId);
    }

    public static Optional<Long> currentBranchId() {
        return currentPrincipal().map(UserPrincipal::getDefaultBranchId);
    }
}
