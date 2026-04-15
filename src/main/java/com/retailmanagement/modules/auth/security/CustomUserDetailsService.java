package com.retailmanagement.modules.auth.security;

import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.model.Account;
import com.retailmanagement.modules.auth.repository.AccountRepository;
import com.retailmanagement.modules.auth.repository.UserBranchAccessRepository;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserBranchAccessRepository userBranchAccessRepository;
    private final SubscriptionAccessService subscriptionAccessService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .<UserDetails>map(this::toPrincipal)
                .orElseGet(() -> accountRepository.findByLoginOrEmailIgnoreCase(username)
                        .map(this::toOnboardingPrincipal)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username)));
    }

    @Transactional
    public UserDetails loadOnboardingUserByUsername(String username) throws UsernameNotFoundException {
        return accountRepository.findByLoginOrEmailIgnoreCase(username)
                .<UserDetails>map(this::toOnboardingPrincipal)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found with username: " + username));
    }

    @Transactional
    public UserDetails loadUserByUsernameAndOrganization(String username, Long organizationId) throws UsernameNotFoundException {
        User user = userRepository.findByLoginAndOrganizationId(username, organizationId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username + " in organization: " + organizationId));
        return toPrincipal(user);
    }

    private UserDetails toPrincipal(User user) {
        user.setBranchAccesses(userBranchAccessRepository.findByUserId(user.getId()));

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                .collect(Collectors.toList());

        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
        });

        return UserPrincipal.create(user, authorities, subscriptionAccessService.currentSnapshot(user.getOrganizationId()));
    }

    private UserDetails toOnboardingPrincipal(Account account) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ONBOARDING"));
        authorities.add(new SimpleGrantedAuthority("org.manage"));
        authorities.add(new SimpleGrantedAuthority("org.view"));

        boolean active = Boolean.TRUE.equals(account.getActive());
        boolean accountNonLocked = !Boolean.TRUE.equals(account.getLocked());
        return UserPrincipal.createAccountOnly(
                account.getId(),
                account.getPerson() == null ? null : account.getPerson().getId(),
                account.getLoginIdentifier(),
                account.getPerson() == null ? null : account.getPerson().getPrimaryEmail(),
                account.getPasswordHash(),
                authorities,
                active,
                accountNonLocked
        );
    }
}
