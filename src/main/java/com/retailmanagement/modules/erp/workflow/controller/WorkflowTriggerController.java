package com.retailmanagement.modules.erp.workflow.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.workflow.dto.WorkflowTriggerDtos;
import com.retailmanagement.modules.erp.workflow.service.WorkflowTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/workflow-triggers")
@RequiredArgsConstructor
@Tag(name = "ERP Workflow", description = "ERP workflow trigger review and dispatch endpoints")
public class WorkflowTriggerController {

    private final WorkflowTriggerService workflowTriggerService;

    @GetMapping("/review")
    @Operation(summary = "Review current workflow triggers")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public ErpApiResponse<WorkflowTriggerDtos.WorkflowTriggerReviewResponse> review(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) LocalDate asOfDate
    ) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(workflowTriggerService.review(orgId, asOfDate));
    }

    @PostMapping("/dispatch")
    @Operation(summary = "Dispatch workflow trigger notifications")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<WorkflowTriggerDtos.WorkflowTriggerDispatchResponse> dispatch(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) LocalDate asOfDate
    ) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(workflowTriggerService.dispatch(orgId, asOfDate), "Workflow triggers dispatched");
    }
}
