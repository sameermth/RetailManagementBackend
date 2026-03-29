package com.retailmanagement.modules.auth.dto.response;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private Long userId;
    private Long accountId;
    private Long organizationId;
    private String organizationCode;
    private String organizationName;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private Set<String> roles;
    private Set<String> permissions;
}
