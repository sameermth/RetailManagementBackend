package com.retailmanagement.modules.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SwitchOrganizationRequest {
    @NotNull(message = "Organization is required")
    private Long organizationId;
}
