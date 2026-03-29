package com.retailmanagement.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private Long id;
    private Long organizationId;
    private String username;
    private String email;
    private Set<String> roles;
    private Long subscriptionVersion;
    private String subscriptionPlanCode;
    private String subscriptionStatus;
    private Set<String> subscriptionFeatures;
}
