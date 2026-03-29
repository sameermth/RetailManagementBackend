package com.retailmanagement.modules.auth.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.modules.auth.dto.request.LoginRequest;
import com.retailmanagement.modules.auth.dto.request.RegisterRequest;
import com.retailmanagement.modules.auth.dto.response.JwtResponse;
import com.retailmanagement.modules.auth.model.Role;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.model.Person;
import com.retailmanagement.modules.auth.model.Account;
import com.retailmanagement.modules.auth.model.OrganizationPersonProfile;
import com.retailmanagement.modules.auth.repository.AccountRepository;
import com.retailmanagement.modules.auth.repository.OrganizationPersonProfileRepository;
import com.retailmanagement.modules.auth.repository.PersonRepository;
import com.retailmanagement.modules.auth.repository.RoleRepository;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.auth.security.CustomUserDetailsService;
import com.retailmanagement.modules.auth.security.JwtTokenProvider;
import com.retailmanagement.modules.auth.security.UserPrincipal;
import com.retailmanagement.modules.auth.service.AuthService;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final OrganizationPersonProfileRepository organizationPersonProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final OrganizationRepository organizationRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final SubscriptionAccessService subscriptionAccessService;

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByLogin(loginRequest.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        if (user.getAccount() != null) {
            user.getAccount().setLastLoginAt(LocalDateTime.now());
            accountRepository.save(user.getAccount());
        }

        UserPrincipal refreshedPrincipal = (UserPrincipal) customUserDetailsService.loadUserByUsername(user.getUsername());
        Authentication jwtAuthentication = new UsernamePasswordAuthenticationToken(
                refreshedPrincipal,
                null,
                refreshedPrincipal.getAuthorities()
        );
        String jwt = tokenProvider.generateToken(jwtAuthentication);

        SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot = subscriptionAccessService.currentSnapshot(user.getOrganizationId());
        return toJwtResponse(user, jwt, subscriptionSnapshot);
    }

    @Override
    @Transactional
    public JwtResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        Organization organization = organizationRepository.findAll().stream()
                .min(Comparator.comparing(Organization::getId))
                .orElseGet(this::createDefaultOrganization);

        String displayName = buildFullName(request.getFirstName(), request.getLastName(), request.getUsername());
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .organizationId(organization.getId())
                .employeeCode(request.getUsername())
                .active(true)
                .build();

        Person person = Person.builder()
                .legalName(displayName)
                .primaryEmail(request.getEmail())
                .primaryPhone(request.getPhone())
                .status("ACTIVE")
                .build();
        person = personRepository.save(person);

        Account account = Account.builder()
                .person(person)
                .loginIdentifier(request.getUsername())
                .passwordHash(encodedPassword)
                .active(true)
                .locked(false)
                .build();
        account = accountRepository.save(account);

        OrganizationPersonProfile organizationPersonProfile = OrganizationPersonProfile.builder()
                .organizationId(organization.getId())
                .person(person)
                .displayName(displayName)
                .emailForOrg(request.getEmail())
                .phoneForOrg(request.getPhone())
                .active(true)
                .build();
        organizationPersonProfileRepository.save(organizationPersonProfile);

        user.setPersonId(person.getId());
        user.setAccountId(account.getId());
        user.setPerson(person);
        user.setAccount(account);
        user.setOrganizationPersonProfile(organizationPersonProfile);

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByCode("VIEWER")
                .orElseGet(() -> roleRepository.findByCode("OWNER")
                        .orElseThrow(() -> new BusinessException("Default role not found")));
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot = subscriptionAccessService.currentSnapshot(savedUser.getOrganizationId());
        List<GrantedAuthority> authorities = savedUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> (GrantedAuthority) () -> permission.getCode())
                .collect(Collectors.toList());
        savedUser.getRoles().forEach(role -> authorities.add(() -> "ROLE_" + role.getCode()));
        UserPrincipal principal = UserPrincipal.create(savedUser, authorities, subscriptionSnapshot);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        return toJwtResponse(savedUser, jwt, subscriptionSnapshot);
    }

    @Override
    public void logout(String token) {
        log.info("Logout user with token: {}", token);
        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("Old password is incorrect");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        if (user.getAccount() != null) {
            user.getAccount().setPasswordHash(encodedPassword);
            accountRepository.save(user.getAccount());
        }
    }

    private String buildFullName(String firstName, String lastName, String fallback) {
        String merged = ((firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim())).trim();
        return merged.isEmpty() ? fallback : merged;
    }

    private Organization createDefaultOrganization() {
        log.warn("No organizations found during registration; creating a default local organization");

        Organization organization = new Organization();
        organization.setCode("LOCAL");
        organization.setName("Local Organization");
        organization.setIsActive(true);

        return organizationRepository.save(organization);
    }

    private JwtResponse toJwtResponse(User user, String jwt, SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getCode())
                .collect(Collectors.toSet());

        return JwtResponse.builder()
                .token(jwt)
                .id(user.getId())
                .organizationId(user.getOrganizationId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .subscriptionVersion(subscriptionSnapshot.subscriptionVersion())
                .subscriptionPlanCode(subscriptionSnapshot.planCode())
                .subscriptionStatus(subscriptionSnapshot.status())
                .subscriptionFeatures(subscriptionSnapshot.featureCodes())
                .build();
    }
}
