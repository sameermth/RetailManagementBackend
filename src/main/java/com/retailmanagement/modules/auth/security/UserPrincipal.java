package com.retailmanagement.modules.auth.security;

import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    private Long id;
    private Long accountId;
    private Long personId;
    private String username;
    private String email;
    private Long organizationId;
    private Long defaultBranchId;
    private Set<Long> accessibleBranchIds;
    private Long subscriptionVersion;
    private String subscriptionPlanCode;
    private String subscriptionStatus;
    private java.util.Set<String> subscriptionFeatures;
    private Boolean onboardingRequired;
    @JsonIgnore
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Boolean active;
    private Boolean accountNonLocked;

    public static UserPrincipal create(
            User user,
            Collection<? extends GrantedAuthority> authorities,
            SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot
    ) {
        boolean accountActive = user.getAccount() == null || Boolean.TRUE.equals(user.getAccount().getActive());
        boolean accountNonLocked = user.getAccount() == null || !Boolean.TRUE.equals(user.getAccount().getLocked());
        return UserPrincipal.builder()
                .id(user.getId())
                .accountId(user.getAccountId())
                .personId(user.getPersonId())
                .username(user.getUsername())
                .email(user.getEmail())
                .organizationId(user.getOrganizationId())
                .defaultBranchId(user.getDefaultBranchId())
                .accessibleBranchIds((user.getBranchAccesses() == null ? List.<com.retailmanagement.modules.auth.model.UserBranchAccess>of() : user.getBranchAccesses()).stream()
                        .map(access -> access.getBranchId())
                        .collect(Collectors.toUnmodifiableSet()))
                .subscriptionVersion(subscriptionSnapshot == null ? 1L : subscriptionSnapshot.subscriptionVersion())
                .subscriptionPlanCode(subscriptionSnapshot == null ? null : subscriptionSnapshot.planCode())
                .subscriptionStatus(subscriptionSnapshot == null ? "NONE" : subscriptionSnapshot.status())
                .subscriptionFeatures(subscriptionSnapshot == null ? java.util.Set.of() : subscriptionSnapshot.featureCodes())
                .onboardingRequired(false)
                .password(user.getPassword())
                .authorities(authorities)
                .active(Boolean.TRUE.equals(user.getActive()) && accountActive)
                .accountNonLocked(accountNonLocked)
                .build();
    }

    public static UserPrincipal create(User user, SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                .collect(Collectors.toList());

        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
        });

        return create(user, authorities, subscriptionSnapshot);
    }

    public static UserPrincipal createAccountOnly(
            Long accountId,
            Long personId,
            String username,
            String email,
            String password,
            Collection<? extends GrantedAuthority> authorities,
            boolean active,
            boolean accountNonLocked
    ) {
        return UserPrincipal.builder()
                .id(null)
                .accountId(accountId)
                .personId(personId)
                .username(username)
                .email(email)
                .organizationId(null)
                .defaultBranchId(null)
                .accessibleBranchIds(Set.of())
                .subscriptionVersion(null)
                .subscriptionPlanCode(null)
                .subscriptionStatus("NONE")
                .subscriptionFeatures(Set.of())
                .onboardingRequired(true)
                .password(password)
                .authorities(authorities)
                .active(active)
                .accountNonLocked(accountNonLocked)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(accountNonLocked);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean hasRole(String roleCode) {
        if (roleCode == null || authorities == null) {
            return false;
        }
        String expected = "ROLE_" + roleCode.trim().toUpperCase();
        return authorities.stream().anyMatch(authority -> expected.equals(authority.getAuthority()));
    }

    public boolean hasBranchAccess(Long branchId) {
        if (branchId == null) {
            return false;
        }
        if (accessibleBranchIds != null && accessibleBranchIds.contains(branchId)) {
            return true;
        }
        return defaultBranchId != null && defaultBranchId.equals(branchId);
    }
}
