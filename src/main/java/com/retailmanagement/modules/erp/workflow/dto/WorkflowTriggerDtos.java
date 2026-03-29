package com.retailmanagement.modules.erp.workflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class WorkflowTriggerDtos {
    private WorkflowTriggerDtos() {}

    public record WorkflowTriggerReviewResponse(
            Long organizationId,
            LocalDate asOfDate,
            int totalTriggers,
            int criticalCount,
            int warningCount,
            List<WorkflowTriggerItemResponse> triggers
    ) {}

    public record WorkflowTriggerDispatchResponse(
            Long organizationId,
            LocalDate asOfDate,
            int reviewedCount,
            int dispatchedCount,
            List<WorkflowTriggerItemResponse> dispatched
    ) {}

    public record WorkflowTriggerItemResponse(
            String triggerType,
            String severity,
            String title,
            String message,
            String referenceType,
            Long referenceId,
            BigDecimal amount,
            LocalDate dueDate,
            Map<String, Object> data
    ) {}
}
