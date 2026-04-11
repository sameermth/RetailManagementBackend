package com.retailmanagement.modules.auth.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.modules.auth.dto.request.LoginRequest;
import com.retailmanagement.modules.auth.dto.request.RefreshTokenRequest;
import com.retailmanagement.modules.auth.dto.request.RegisterRequest;
import com.retailmanagement.modules.auth.dto.response.JwtResponse;
import com.retailmanagement.modules.auth.model.AuthRefreshSession;
import com.retailmanagement.modules.auth.model.ClientType;
import com.retailmanagement.modules.auth.model.Role;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.model.Person;
import com.retailmanagement.modules.auth.model.Account;
import com.retailmanagement.modules.auth.model.OrganizationPersonProfile;
import com.retailmanagement.modules.auth.repository.AccountRepository;
import com.retailmanagement.modules.auth.repository.AuthRefreshSessionRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
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
    private final AuthRefreshSessionRepository authRefreshSessionRepository;
    private final OrganizationPersonProfileRepository organizationPersonProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final OrganizationRepository organizationRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final SubscriptionAccessService subscriptionAccessService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.refresh.mobile.expiration-days:30}")
    private int mobileRefreshExpirationDays;

    @Value("${auth.refresh.web.expiration-days:1}")
    private int webRefreshExpirationDays;

    @Value("${auth.refresh.inactivity-days:7}")
    private int refreshInactivityDays;

    @Override
    @Transactional
    public JwtResponse login(LoginRequest loginRequest, String userAgent, String deviceId, String deviceName) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        List<User> memberships = userRepository.findAllByLogin(loginRequest.getUsername()).stream()
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .toList();
        if (memberships.isEmpty()) {
            throw new BusinessException("User not found");
        }
        User user = resolveMembership(memberships, loginRequest.getOrganizationId());

        if (user.getAccount() != null) {
            user.getAccount().setLastLoginAt(LocalDateTime.now());
            accountRepository.save(user.getAccount());
        }
        ClientType clientType = resolveClientType(loginRequest.getClientType(), userAgent);
        JwtIssue jwtIssue = issueAccessToken(user);
        RefreshTokenIssue refreshTokenIssue = createRefreshSession(user, clientType, userAgent, deviceId, deviceName);
        SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot = subscriptionAccessService.currentSnapshot(user.getOrganizationId());
        return toJwtResponse(
                user,
                jwtIssue.accessToken(),
                subscriptionSnapshot,
                memberships,
                refreshTokenIssue.refreshToken(),
                refreshTokenIssue.expiresAt(),
                clientType,
                jwtIssue.accessTokenExpiresAt()
        );
    }

    @Override
    @Transactional
    public JwtResponse refresh(RefreshTokenRequest refreshTokenRequest, String userAgent, String deviceId, String deviceName) {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        AuthRefreshSession session = authRefreshSessionRepository.findByRefreshTokenHash(hashToken(refreshToken))
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        LocalDateTime now = LocalDateTime.now();
        if (session.getRevokedAt() != null) {
            throw new BusinessException("Refresh token has been revoked");
        }
        if (session.getExpiresAt() == null || session.getExpiresAt().isBefore(now)) {
            revokeSession(session, "EXPIRED");
            throw new BusinessException("Refresh token has expired");
        }
        if (session.getLastUsedAt() != null && session.getLastUsedAt().plusDays(refreshInactivityDays).isBefore(now)) {
            revokeSession(session, "INACTIVE_TIMEOUT");
            throw new BusinessException("Refresh token expired due to inactivity");
        }

        User user = userRepository.findByIdAndOrganizationId(session.getUserId(), session.getOrganizationId())
                .filter(member -> Boolean.TRUE.equals(member.getActive()))
                .orElseThrow(() -> new BusinessException("User membership is no longer active"));

        JwtIssue jwtIssue = issueAccessToken(user);
        RefreshTokenIssue refreshTokenIssue = rotateRefreshSession(session, userAgent, deviceId, deviceName);
        SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot = subscriptionAccessService.currentSnapshot(user.getOrganizationId());
        List<User> memberships = userRepository.findAllByLogin(user.getUsername()).stream()
                .filter(member -> Boolean.TRUE.equals(member.getActive()))
                .toList();
        return toJwtResponse(
                user,
                jwtIssue.accessToken(),
                subscriptionSnapshot,
                memberships,
                refreshTokenIssue.refreshToken(),
                refreshTokenIssue.expiresAt(),
                refreshTokenIssue.clientType(),
                jwtIssue.accessTokenExpiresAt()
        );
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
        JwtIssue jwtIssue = issueAccessToken(savedUser);
        RefreshTokenIssue refreshTokenIssue = createRefreshSession(savedUser, ClientType.WEB, null, null, null);
        return toJwtResponse(
                savedUser,
                jwtIssue.accessToken(),
                subscriptionSnapshot,
                List.of(savedUser),
                refreshTokenIssue.refreshToken(),
                refreshTokenIssue.expiresAt(),
                ClientType.WEB,
                jwtIssue.accessTokenExpiresAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponse switchOrganization(Long organizationId) {
        Object principalObject = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principalObject instanceof UserPrincipal principal)) {
            throw new BusinessException("No authenticated user available for organization switch");
        }

        List<User> memberships = userRepository.findAllByLogin(principal.getUsername()).stream()
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .toList();
        if (memberships.isEmpty()) {
            throw new BusinessException("No active organization memberships found");
        }
        User user = resolveMembership(memberships, organizationId);
        UserPrincipal switchedPrincipal = (UserPrincipal) customUserDetailsService
                .loadUserByUsernameAndOrganization(user.getUsername(), user.getOrganizationId());
        Authentication switchedAuthentication = new UsernamePasswordAuthenticationToken(
                switchedPrincipal,
                null,
                switchedPrincipal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(switchedAuthentication);
        String jwt = tokenProvider.generateToken(switchedAuthentication);
        LocalDateTime accessExpiresAt = LocalDateTime.now().plusNanos((long) tokenProvider.getJwtExpirationMillis() * 1_000_000L);
        SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot = subscriptionAccessService.currentSnapshot(user.getOrganizationId());
        return toJwtResponse(user, jwt, subscriptionSnapshot, memberships, null, null, null, accessExpiresAt);
    }

    @Override
    @Transactional
    public void logout(String token, RefreshTokenRequest refreshTokenRequest) {
        log.info("Logout user with token: {}", token);
        if (refreshTokenRequest != null && refreshTokenRequest.getRefreshToken() != null && !refreshTokenRequest.getRefreshToken().isBlank()) {
            authRefreshSessionRepository.findByRefreshTokenHash(hashToken(refreshTokenRequest.getRefreshToken().trim()))
                    .ifPresent(session -> revokeSession(session, "LOGOUT"));
        }
        if (token != null && !token.isBlank()) {
            String accessToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            if (tokenProvider.validateToken(accessToken)) {
                String username = tokenProvider.getUsernameFromToken(accessToken);
                Long organizationId = tokenProvider.getOrganizationIdFromToken(accessToken);
                if (username != null && organizationId != null) {
                    userRepository.findByLoginAndOrganizationId(username, organizationId)
                            .ifPresent(user -> authRefreshSessionRepository
                                    .findByAccountIdAndRevokedAtIsNullAndExpiresAtAfter(user.getAccountId(), LocalDateTime.now())
                                    .forEach(session -> revokeSession(session, "LOGOUT_ALL")));
                }
            }
        }
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

    private JwtResponse toJwtResponse(User user, String jwt, SubscriptionAccessService.SubscriptionSnapshot subscriptionSnapshot,
                                      List<User> memberships, String refreshToken, LocalDateTime refreshTokenExpiresAt,
                                      ClientType clientType, LocalDateTime accessTokenExpiresAt) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getCode())
                .collect(Collectors.toSet());
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Organization organization = organizationRepository.findById(user.getOrganizationId())
                .orElseThrow(() -> new BusinessException("Organization not found: " + user.getOrganizationId()));

        return JwtResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .refreshTokenExpiresAt(refreshTokenExpiresAt)
                .clientType(clientType == null ? null : clientType.name())
                .id(user.getId())
                .organizationId(user.getOrganizationId())
                .organizationCode(organization.getCode())
                .organizationName(organization.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .permissions(permissions)
                .subscriptionVersion(subscriptionSnapshot.subscriptionVersion())
                .subscriptionPlanCode(subscriptionSnapshot.planCode())
                .subscriptionStatus(subscriptionSnapshot.status())
                .subscriptionFeatures(subscriptionSnapshot.featureCodes())
                .memberships(toMembershipSummaries(memberships))
                .build();
    }

    private JwtIssue issueAccessToken(User user) {
        UserPrincipal refreshedPrincipal = (UserPrincipal) customUserDetailsService
                .loadUserByUsernameAndOrganization(user.getUsername(), user.getOrganizationId());
        Authentication jwtAuthentication = new UsernamePasswordAuthenticationToken(
                refreshedPrincipal,
                null,
                refreshedPrincipal.getAuthorities()
        );
        String jwt = tokenProvider.generateToken(jwtAuthentication);
        return new JwtIssue(
                jwt,
                LocalDateTime.now().plusNanos((long) tokenProvider.getJwtExpirationMillis() * 1_000_000L)
        );
    }

    private RefreshTokenIssue createRefreshSession(User user, ClientType clientType, String userAgent, String deviceId, String deviceName) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(refreshExpirationDays(clientType));
        String rawToken = generateOpaqueToken();
        AuthRefreshSession session = AuthRefreshSession.builder()
                .accountId(user.getAccountId())
                .organizationId(user.getOrganizationId())
                .userId(user.getId())
                .clientType(clientType)
                .deviceId(trimToNull(deviceId))
                .deviceName(trimToNull(deviceName))
                .userAgent(trimToNull(userAgent))
                .refreshTokenHash(hashToken(rawToken))
                .issuedAt(now)
                .lastUsedAt(now)
                .expiresAt(expiresAt)
                .build();
        authRefreshSessionRepository.save(session);
        return new RefreshTokenIssue(rawToken, expiresAt, clientType);
    }

    private RefreshTokenIssue rotateRefreshSession(AuthRefreshSession session, String userAgent, String deviceId, String deviceName) {
        String rawToken = generateOpaqueToken();
        session.setRefreshTokenHash(hashToken(rawToken));
        session.setLastUsedAt(LocalDateTime.now());
        if (trimToNull(userAgent) != null) session.setUserAgent(trimToNull(userAgent));
        if (trimToNull(deviceId) != null) session.setDeviceId(trimToNull(deviceId));
        if (trimToNull(deviceName) != null) session.setDeviceName(trimToNull(deviceName));
        authRefreshSessionRepository.save(session);
        return new RefreshTokenIssue(rawToken, session.getExpiresAt(), session.getClientType());
    }

    private void revokeSession(AuthRefreshSession session, String reason) {
        session.setRevokedAt(LocalDateTime.now());
        session.setRevokeReason(reason);
        authRefreshSessionRepository.save(session);
    }

    private int refreshExpirationDays(ClientType clientType) {
        if (clientType == ClientType.MOBILE || clientType == ClientType.TABLET) {
            return mobileRefreshExpirationDays;
        }
        return webRefreshExpirationDays;
    }

    private ClientType resolveClientType(String requestedClientType, String userAgent) {
        ClientType requested = null;
        try {
            requested = ClientType.fromNullable(requestedClientType);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Unsupported clientType. Allowed values: WEB, MOBILE, TABLET");
        }
        if (requested != null) {
            return requested;
        }
        String ua = userAgent == null ? "" : userAgent.toLowerCase();
        if (ua.contains("ipad") || ua.contains("tablet") || (ua.contains("android") && !ua.contains("mobile"))) {
            return ClientType.TABLET;
        }
        if (ua.contains("iphone") || ua.contains("mobile") || ua.contains("android")) {
            return ClientType.MOBILE;
        }
        return ClientType.WEB;
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User resolveMembership(List<User> memberships, Long requestedOrganizationId) {
        if (requestedOrganizationId != null) {
            return memberships.stream()
                    .filter(user -> requestedOrganizationId.equals(user.getOrganizationId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("No active membership found for organization " + requestedOrganizationId));
        }
        return memberships.stream()
                .sorted(Comparator
                        .comparingInt((User user) -> roleRank(user.getRole() == null ? null : user.getRole().getCode()))
                        .thenComparing(User::getId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No active membership found"));
    }

    private List<JwtResponse.MembershipSummary> toMembershipSummaries(List<User> memberships) {
        return memberships.stream()
                .sorted(Comparator.comparing(User::getOrganizationId))
                .map(user -> {
                    Organization organization = organizationRepository.findById(user.getOrganizationId())
                            .orElse(null);
                    return JwtResponse.MembershipSummary.builder()
                            .userId(user.getId())
                            .organizationId(user.getOrganizationId())
                            .organizationCode(organization == null ? null : organization.getCode())
                            .organizationName(organization == null ? null : organization.getName())
                            .defaultBranchId(user.getDefaultBranchId())
                            .roleCode(user.getRole() == null ? null : user.getRole().getCode())
                            .roleName(user.getRole() == null ? null : user.getRole().getName())
                            .active(user.getActive())
                            .build();
                })
                .toList();
    }

    private int roleRank(String roleCode) {
        if (roleCode == null) {
            return 99;
        }
        return switch (roleCode.trim().toUpperCase()) {
            case "OWNER" -> 0;
            case "ADMIN" -> 1;
            case "ACCOUNTANT" -> 2;
            case "STORE_MANAGER" -> 3;
            default -> 10;
        };
    }

    private record JwtIssue(String accessToken, LocalDateTime accessTokenExpiresAt) {}

    private record RefreshTokenIssue(String refreshToken, LocalDateTime expiresAt, ClientType clientType) {}
}
