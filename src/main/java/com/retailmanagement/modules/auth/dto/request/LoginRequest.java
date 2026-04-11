package com.retailmanagement.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private Long organizationId;

    // Optional override; when missing, backend derives client type from headers.
    private String clientType;
}
