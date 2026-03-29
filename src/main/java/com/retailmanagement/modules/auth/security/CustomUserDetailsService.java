package com.retailmanagement.modules.auth.security;

import com.retailmanagement.modules.auth.model.User;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserBranchAccessRepository userBranchAccessRepository;
    private final SubscriptionAccessService subscriptionAccessService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return toPrincipal(user);
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
}
