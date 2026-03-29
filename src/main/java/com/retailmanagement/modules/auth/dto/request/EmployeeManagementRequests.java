package com.retailmanagement.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class EmployeeManagementRequests {
    private EmployeeManagementRequests() {}

    public record CreateEmployeeRequest(
            @NotNull Long organizationId,
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String fullName,
            String email,
            String phone,
            @NotBlank String roleCode,
            String employeeCode,
            Long defaultBranchId,
            @NotEmpty List<Long> branchIds,
            Boolean active
    ) {}

    public record UpdateEmployeeRequest(
            String fullName,
            String email,
            String phone,
            String roleCode,
            String employeeCode,
            Long defaultBranchId,
            List<Long> branchIds,
            Boolean active
    ) {}
}
