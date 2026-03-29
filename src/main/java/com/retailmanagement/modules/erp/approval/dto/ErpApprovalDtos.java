package com.retailmanagement.modules.erp.approval.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ErpApprovalDtos {
    private ErpApprovalDtos() {}

    public record CreateApprovalRuleRequest(
            @NotBlank String entityType,
            @NotBlank String approvalType,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Long approverRoleId,
            Integer priorityOrder,
            Boolean active
    ) {}

    public record CreateApprovalRequest(
            @NotBlank String entityType,
            @NotNull Long entityId,
            String entityNumber,
            @NotBlank String approvalType,
            String requestReason,
            Long currentApproverUserId,
            String currentApproverRoleSnapshot
    ) {}

    public record ApprovalActionRequest(
            @NotBlank String remarks
    ) {}

    public record ApprovalEvaluationQuery(
            @NotBlank String entityType,
            @NotNull Long entityId,
            String approvalType
    ) {}

    public record ApprovalRuleResponse(
            Long id,
            Long organizationId,
            Long branchId,
            String entityType,
            String approvalType,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Long approverRoleId,
            Integer priorityOrder,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ApprovalRequestResponse(
            Long id,
            Long organizationId,
            Long branchId,
            String entityType,
            Long entityId,
            String entityNumber,
            String approvalType,
            String status,
            Long requestedBy,
            LocalDateTime requestedAt,
            Long currentApproverUserId,
            String currentApproverRoleSnapshot,
            String requestReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ApprovalHistoryResponse(
            Long id,
            Long approvalRequestId,
            Long approverUserId,
            String action,
            String approverRoleSnapshot,
            String remarks,
            LocalDateTime actionAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ApprovalRequestDetailsResponse(
            ApprovalRequestResponse request,
            List<ApprovalHistoryResponse> history
    ) {}

    public record ApprovalEvaluationResponse(
            boolean approvalRequired,
            String entityType,
            Long entityId,
            String entityNumber,
            String approvalType,
            Long branchId,
            java.math.BigDecimal entityAmount,
            Long matchedRuleId,
            Long approverRoleId,
            String approverRoleCode,
            String approverRoleName,
            boolean pendingRequestExists,
            Long pendingRequestId
    ) {}

    public record ApprovalQueueSummaryItemResponse(
            String entityType,
            long pendingCount
    ) {}

    public record ApprovalQueueSummaryResponse(
            long totalPending,
            List<ApprovalQueueSummaryItemResponse> pendingByEntityType
    ) {}
}
