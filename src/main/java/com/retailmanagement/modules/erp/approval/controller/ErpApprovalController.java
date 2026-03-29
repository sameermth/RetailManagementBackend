package com.retailmanagement.modules.erp.approval.controller;

import com.retailmanagement.modules.erp.approval.dto.ErpApprovalDtos;
import com.retailmanagement.modules.erp.approval.entity.ApprovalHistory;
import com.retailmanagement.modules.erp.approval.entity.ApprovalRequest;
import com.retailmanagement.modules.erp.approval.entity.ApprovalRule;
import com.retailmanagement.modules.erp.approval.service.ErpApprovalService;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/approvals")
@RequiredArgsConstructor
@Tag(name = "ERP Approvals", description = "ERP approval rules and approval request endpoints")
public class ErpApprovalController {

    private final ErpApprovalService erpApprovalService;

    @GetMapping("/rules")
    @Operation(summary = "List approval rules")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<List<ErpApprovalDtos.ApprovalRuleResponse>> listRules(@RequestParam Long organizationId,
                                                                                @RequestParam(required = false) String entityType,
                                                                                @RequestParam(required = false) Long branchId) {
        return ErpApiResponse.ok(erpApprovalService.listRules(organizationId, entityType, branchId).stream().map(this::toRuleResponse).toList());
    }

    @PostMapping("/rules")
    @Operation(summary = "Create approval rule")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<ErpApprovalDtos.ApprovalRuleResponse> createRule(@RequestParam Long organizationId,
                                                                           @RequestParam Long branchId,
                                                                           @RequestBody @Valid ErpApprovalDtos.CreateApprovalRuleRequest request) {
        return ErpApiResponse.ok(toRuleResponse(erpApprovalService.createRule(organizationId, branchId, request)), "Approval rule created");
    }

    @GetMapping("/requests")
    @Operation(summary = "List approval requests")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<List<ErpApprovalDtos.ApprovalRequestResponse>> listRequests(@RequestParam Long organizationId,
                                                                                      @RequestParam(required = false) String status) {
        return ErpApiResponse.ok(erpApprovalService.listRequests(organizationId, status).stream().map(this::toRequestResponse).toList());
    }

    @GetMapping("/requests/{id}")
    @Operation(summary = "Get approval request details")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<ErpApprovalDtos.ApprovalRequestDetailsResponse> getRequest(@PathVariable Long id) {
        return ErpApiResponse.ok(toDetailsResponse(erpApprovalService.getRequest(id)));
    }

    @PostMapping("/requests")
    @Operation(summary = "Create approval request")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<ErpApprovalDtos.ApprovalRequestResponse> createRequest(@RequestParam Long organizationId,
                                                                                 @RequestParam Long branchId,
                                                                                 @RequestBody @Valid ErpApprovalDtos.CreateApprovalRequest request) {
        return ErpApiResponse.ok(toRequestResponse(erpApprovalService.createRequest(organizationId, branchId, request)), "Approval request created");
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate whether an entity needs approval")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<ErpApprovalDtos.ApprovalEvaluationResponse> evaluate(@RequestParam Long organizationId,
                                                                               @RequestBody @Valid ErpApprovalDtos.ApprovalEvaluationQuery query) {
        return ErpApiResponse.ok(toEvaluationResponse(erpApprovalService.evaluate(organizationId, query)));
    }

    @GetMapping("/requests/summary")
    @Operation(summary = "Get pending approval queue summary")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<ErpApprovalDtos.ApprovalQueueSummaryResponse> queueSummary(@RequestParam Long organizationId) {
        return ErpApiResponse.ok(toQueueSummaryResponse(erpApprovalService.queueSummary(organizationId)));
    }

    @PostMapping("/requests/{id}/approve")
    @Operation(summary = "Approve approval request")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<ErpApprovalDtos.ApprovalRequestResponse> approve(@PathVariable Long id,
                                                                           @RequestParam Long organizationId,
                                                                           @RequestBody @Valid ErpApprovalDtos.ApprovalActionRequest request) {
        return ErpApiResponse.ok(toRequestResponse(erpApprovalService.approve(organizationId, id, request.remarks())), "Approval request approved");
    }

    @PostMapping("/requests/{id}/reject")
    @Operation(summary = "Reject approval request")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<ErpApprovalDtos.ApprovalRequestResponse> reject(@PathVariable Long id,
                                                                          @RequestParam Long organizationId,
                                                                          @RequestBody @Valid ErpApprovalDtos.ApprovalActionRequest request) {
        return ErpApiResponse.ok(toRequestResponse(erpApprovalService.reject(organizationId, id, request.remarks())), "Approval request rejected");
    }

    @PostMapping("/requests/{id}/cancel")
    @Operation(summary = "Cancel approval request")
    @PreAuthorize("hasAuthority('approval.manage')")
    public ErpApiResponse<ErpApprovalDtos.ApprovalRequestResponse> cancel(@PathVariable Long id,
                                                                          @RequestParam Long organizationId,
                                                                          @RequestBody @Valid ErpApprovalDtos.ApprovalActionRequest request) {
        return ErpApiResponse.ok(toRequestResponse(erpApprovalService.cancel(organizationId, id, request.remarks())), "Approval request cancelled");
    }

    private ErpApprovalDtos.ApprovalRuleResponse toRuleResponse(ApprovalRule rule) {
        return new ErpApprovalDtos.ApprovalRuleResponse(rule.getId(), rule.getOrganizationId(), rule.getBranchId(),
                rule.getEntityType(), rule.getApprovalType(), rule.getMinAmount(), rule.getMaxAmount(), rule.getApproverRoleId(),
                rule.getPriorityOrder(), rule.isActive(), rule.getCreatedAt(), rule.getUpdatedAt());
    }

    private ErpApprovalDtos.ApprovalRequestResponse toRequestResponse(ApprovalRequest request) {
        return new ErpApprovalDtos.ApprovalRequestResponse(request.getId(), request.getOrganizationId(), request.getBranchId(),
                request.getEntityType(), request.getEntityId(), request.getEntityNumber(), request.getApprovalType(),
                request.getStatus(), request.getRequestedBy(), request.getRequestedAt(), request.getCurrentApproverUserId(),
                request.getCurrentApproverRoleSnapshot(), request.getRequestReason(), request.getCreatedAt(), request.getUpdatedAt());
    }

    private ErpApprovalDtos.ApprovalHistoryResponse toHistoryResponse(ApprovalHistory history) {
        return new ErpApprovalDtos.ApprovalHistoryResponse(history.getId(), history.getApprovalRequestId(), history.getApproverUserId(),
                history.getAction(), history.getApproverRoleSnapshot(), history.getRemarks(), history.getActionAt(),
                history.getCreatedAt(), history.getUpdatedAt());
    }

    private ErpApprovalDtos.ApprovalRequestDetailsResponse toDetailsResponse(ErpApprovalService.ApprovalRequestDetails details) {
        return new ErpApprovalDtos.ApprovalRequestDetailsResponse(
                toRequestResponse(details.request()),
                details.history().stream().map(this::toHistoryResponse).toList()
        );
    }

    private ErpApprovalDtos.ApprovalEvaluationResponse toEvaluationResponse(ErpApprovalService.ApprovalEvaluation evaluation) {
        return new ErpApprovalDtos.ApprovalEvaluationResponse(
                evaluation.approvalRequired(),
                evaluation.entityType(),
                evaluation.entityId(),
                evaluation.entityNumber(),
                evaluation.approvalType(),
                evaluation.branchId(),
                evaluation.entityAmount(),
                evaluation.matchedRuleId(),
                evaluation.approverRoleId(),
                evaluation.approverRoleCode(),
                evaluation.approverRoleName(),
                evaluation.pendingRequestExists(),
                evaluation.pendingRequestId()
        );
    }

    private ErpApprovalDtos.ApprovalQueueSummaryResponse toQueueSummaryResponse(ErpApprovalService.ApprovalQueueSummary summary) {
        return new ErpApprovalDtos.ApprovalQueueSummaryResponse(
                summary.totalPending(),
                summary.pendingByEntityType().stream()
                        .map(item -> new ErpApprovalDtos.ApprovalQueueSummaryItemResponse(item.entityType(), item.pendingCount()))
                        .toList()
        );
    }
}
