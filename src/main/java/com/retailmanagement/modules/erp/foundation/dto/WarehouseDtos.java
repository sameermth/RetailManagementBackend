package com.retailmanagement.modules.erp.foundation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public final class WarehouseDtos {
    private WarehouseDtos() {}

    public record CreateWarehouseRequest(
            @NotNull Long organizationId,
            @NotNull Long branchId,
            @NotBlank String code,
            @NotBlank String name,
            Boolean isPrimary,
            Boolean isActive
    ) {}

    public record UpdateWarehouseRequest(
            String code,
            String name,
            Boolean isPrimary,
            Boolean isActive
    ) {}

    public record WarehouseResponse(
            Long id,
            Long organizationId,
            Long branchId,
            String code,
            String name,
            Boolean isPrimary,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
