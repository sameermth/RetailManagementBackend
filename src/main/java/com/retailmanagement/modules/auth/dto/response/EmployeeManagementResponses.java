package com.retailmanagement.modules.auth.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public final class EmployeeManagementResponses {
    private EmployeeManagementResponses() {}

    public record BranchAccessSummary(
            Long branchId,
            Boolean isDefault
    ) {}

    public record EmployeeResponse(
            Long id,
            Long organizationId,
            Long accountId,
            Long personId,
            String username,
            String fullName,
            String email,
            String phone,
            String roleCode,
            String roleName,
            String employeeCode,
            Long defaultBranchId,
            List<BranchAccessSummary> branchAccess,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
