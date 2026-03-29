package com.retailmanagement.modules.erp.foundation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public final class BranchDtos {
    private BranchDtos() {}

    public record CreateBranchRequest(
            @NotNull Long organizationId,
            @NotBlank String code,
            @NotBlank String name,
            String phone,
            String email,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            Boolean isActive
    ) {}

    public record UpdateBranchRequest(
            String code,
            String name,
            String phone,
            String email,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            Boolean isActive
    ) {}

    public record BranchResponse(
            Long id,
            Long organizationId,
            String code,
            String name,
            String phone,
            String email,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
