package com.retailmanagement.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    @Builder.Default
    private String type = "Bearer";
    private LocalDateTime accessTokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;
    private String clientType;
    private Long id;
    private Long organizationId;
    private String organizationCode;
    private String organizationName;
    private String username;
    private String email;
    private Boolean onboardingRequired;
    private Set<String> roles;
    private Set<String> permissions;
    private Long subscriptionVersion;
    private String subscriptionPlanCode;
    private String subscriptionStatus;
    private Set<String> subscriptionFeatures;
    private List<MembershipSummary> memberships;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MembershipSummary {
        private Long userId;
        private Long organizationId;
        private String organizationCode;
        private String organizationName;
        private Long defaultBranchId;
        private String roleCode;
        private String roleName;
        private Boolean active;
    }
}
