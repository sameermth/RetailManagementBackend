package com.retailmanagement.modules.erp.approval.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.auth.model.Role;
import com.retailmanagement.modules.auth.repository.RoleRepository;
import com.retailmanagement.modules.erp.approval.dto.ErpApprovalDtos;
import com.retailmanagement.modules.erp.approval.entity.ApprovalHistory;
import com.retailmanagement.modules.erp.approval.entity.ApprovalRequest;
import com.retailmanagement.modules.erp.approval.entity.ApprovalRule;
import com.retailmanagement.modules.erp.approval.repository.ApprovalHistoryRepository;
import com.retailmanagement.modules.erp.approval.repository.ApprovalRequestRepository;
import com.retailmanagement.modules.erp.approval.repository.ApprovalRuleRepository;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.expense.entity.Expense;
import com.retailmanagement.modules.erp.expense.entity.ExpenseCategory;
import com.retailmanagement.modules.erp.expense.repository.ExpenseCategoryRepository;
import com.retailmanagement.modules.erp.expense.repository.ExpenseRepository;
import com.retailmanagement.modules.erp.finance.repository.AccountRepository;
import com.retailmanagement.modules.erp.finance.service.ErpAccountingPostingService;
import com.retailmanagement.modules.erp.inventory.entity.StockAdjustment;
import com.retailmanagement.modules.erp.inventory.entity.StockTransfer;
import com.retailmanagement.modules.erp.inventory.repository.StockAdjustmentRepository;
import com.retailmanagement.modules.erp.inventory.repository.StockTransferRepository;
import com.retailmanagement.modules.erp.inventory.service.InventoryReservationService;
import com.retailmanagement.modules.erp.inventory.service.StockAdjustmentPostingService;
import com.retailmanagement.modules.erp.inventory.service.StockTransferPostingService;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrder;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseOrderRepository;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturn;
import com.retailmanagement.modules.erp.returns.entity.SalesReturn;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnRepository;
import com.retailmanagement.modules.erp.returns.service.ReturnPostingService;
import com.retailmanagement.modules.erp.service.entity.WarrantyClaim;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.service.repository.WarrantyClaimRepository;
import com.retailmanagement.modules.erp.sales.service.SalesInvoicePostingService;
import com.retailmanagement.modules.erp.service.entity.ServiceReplacement;
import com.retailmanagement.modules.erp.service.repository.ServiceReplacementRepository;
import com.retailmanagement.modules.erp.service.service.ServiceReplacementPostingService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ErpApprovalService {

    private final ApprovalRuleRepository approvalRuleRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final StockAdjustmentPostingService stockAdjustmentPostingService;
    private final StockTransferRepository stockTransferRepository;
    private final StockTransferPostingService stockTransferPostingService;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final ServiceReplacementRepository serviceReplacementRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final ErpAccountingPostingService accountingPostingService;
    private final InventoryReservationService inventoryReservationService;
    private final SalesInvoicePostingService salesInvoicePostingService;
    private final ServiceReplacementPostingService serviceReplacementPostingService;
    private final ReturnPostingService returnPostingService;
    private final AuditEventWriter auditEventWriter;

    @Transactional(readOnly = true)
    public List<ApprovalRule> listRules(Long organizationId, String entityType, Long branchId) {
        List<ApprovalRule> rules = (entityType == null || entityType.isBlank())
                ? approvalRuleRepository.findByOrganizationIdAndActiveTrueOrderByPriorityOrderAsc(organizationId)
                : approvalRuleRepository.findByOrganizationIdAndEntityTypeAndActiveTrueOrderByPriorityOrderAsc(organizationId, entityType);
        if (branchId == null) {
            return rules;
        }
        return rules.stream()
                .filter(rule -> rule.getBranchId() == null || branchId.equals(rule.getBranchId()))
                .toList();
    }

    public ApprovalRule createRule(Long organizationId, Long branchId, ErpApprovalDtos.CreateApprovalRuleRequest request) {
        ApprovalRule rule = new ApprovalRule();
        rule.setOrganizationId(organizationId);
        rule.setBranchId(branchId);
        rule.setEntityType(request.entityType());
        rule.setApprovalType(request.approvalType());
        rule.setMinAmount(request.minAmount());
        rule.setMaxAmount(request.maxAmount());
        rule.setApproverRoleId(request.approverRoleId());
        rule.setPriorityOrder(request.priorityOrder() == null ? 1 : request.priorityOrder());
        rule.setActive(request.active() == null || request.active());
        return approvalRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> listRequests(Long organizationId, String status) {
        if (status == null || status.isBlank()) {
            return approvalRequestRepository.findTop100ByOrganizationIdOrderByRequestedAtDescIdDesc(organizationId);
        }
        return approvalRequestRepository.findTop100ByOrganizationIdAndStatusOrderByRequestedAtDescIdDesc(organizationId, status);
    }

    @Transactional(readOnly = true)
    public ApprovalRequestDetails getRequest(Long id) {
        ApprovalRequest request = approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));
        List<ApprovalHistory> history = approvalHistoryRepository.findByApprovalRequestIdOrderByActionAtAscIdAsc(id);
        return new ApprovalRequestDetails(request, history);
    }

    public ApprovalRequest createRequest(Long organizationId, Long branchId, ErpApprovalDtos.CreateApprovalRequest request) {
        approvalRequestRepository.findByOrganizationIdAndEntityTypeAndEntityIdAndStatus(organizationId, request.entityType(), request.entityId(), "PENDING")
                .ifPresent(existing -> { throw new BusinessException("A pending approval already exists for this entity"); });

        EntityRef entityRef = resolveEntity(organizationId, request.entityType(), request.entityId());
        ApprovalRule matchedRule = findMatchingRule(organizationId, entityRef, request.approvalType()).orElse(null);
        Role approverRole = matchedRule == null || matchedRule.getApproverRoleId() == null
                ? null
                : roleRepository.findById(matchedRule.getApproverRoleId()).orElse(null);

        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setOrganizationId(organizationId);
        approvalRequest.setBranchId(entityRef.branchId() == null ? branchId : entityRef.branchId());
        approvalRequest.setEntityType(request.entityType());
        approvalRequest.setEntityId(request.entityId());
        approvalRequest.setEntityNumber(request.entityNumber() == null || request.entityNumber().isBlank()
                ? entityRef.entityNumber() : request.entityNumber());
        approvalRequest.setApprovalType(request.approvalType());
        approvalRequest.setStatus("PENDING");
        approvalRequest.setRequestedBy(ErpSecurityUtils.currentUserId().orElse(1L));
        approvalRequest.setRequestedAt(LocalDateTime.now());
        approvalRequest.setCurrentApproverUserId(request.currentApproverUserId());
        approvalRequest.setCurrentApproverRoleSnapshot(resolveApproverRoleSnapshot(request.currentApproverRoleSnapshot(), approverRole));
        approvalRequest.setRequestReason(request.requestReason());
        approvalRequest = approvalRequestRepository.save(approvalRequest);

        insertHistory(approvalRequest.getId(), "REQUESTED", request.requestReason());
        transitionEntityOnRequested(entityRef);

        auditEventWriter.write(
                organizationId,
                branchId,
                "APPROVAL_REQUEST_CREATED",
                "approval_request",
                approvalRequest.getId(),
                approvalRequest.getEntityNumber(),
                "REQUEST",
                null,
                null,
                entityRef.supplierId(),
                "Approval request created for " + request.entityType(),
                json("requestId", approvalRequest.getId(), "entityType", request.entityType(), "entityId", request.entityId(), "approvalType", request.approvalType())
        );
        return approvalRequest;
    }

    @Transactional(readOnly = true)
    public ApprovalEvaluation evaluate(Long organizationId, ErpApprovalDtos.ApprovalEvaluationQuery query) {
        EntityRef entityRef = resolveEntity(organizationId, query.entityType(), query.entityId());
        Optional<ApprovalRule> matchedRule = findMatchingRule(organizationId, entityRef, query.approvalType());
        Optional<ApprovalRequest> pendingRequest = approvalRequestRepository.findByOrganizationIdAndEntityTypeAndEntityIdAndStatus(
                organizationId, query.entityType(), query.entityId(), "PENDING");
        Role approverRole = matchedRule.flatMap(rule -> rule.getApproverRoleId() == null ? Optional.empty() : roleRepository.findById(rule.getApproverRoleId()))
                .orElse(null);
        return new ApprovalEvaluation(
                matchedRule.isPresent(),
                entityRef.entityType(),
                entityRef.entityId(),
                entityRef.entityNumber(),
                query.approvalType(),
                entityRef.branchId(),
                entityRef.amount(),
                matchedRule.map(ApprovalRule::getId).orElse(null),
                matchedRule.map(ApprovalRule::getApproverRoleId).orElse(null),
                approverRole == null ? null : approverRole.getCode(),
                approverRole == null ? null : approverRole.getName(),
                pendingRequest.isPresent(),
                pendingRequest.map(ApprovalRequest::getId).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public ApprovalQueueSummary queueSummary(Long organizationId) {
        List<ApprovalRequest> pending = approvalRequestRepository.findByOrganizationIdAndStatusOrderByRequestedAtDescIdDesc(organizationId, "PENDING");
        Map<String, Long> grouped = pending.stream()
                .collect(Collectors.groupingBy(ApprovalRequest::getEntityType, Collectors.counting()));
        List<ApprovalQueueSummaryItem> items = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ApprovalQueueSummaryItem(entry.getKey(), entry.getValue()))
                .toList();
        return new ApprovalQueueSummary(pending.size(), items);
    }

    public ApprovalRequest approve(Long organizationId, Long requestId, String remarks) {
        ApprovalRequest request = findOwnedRequest(organizationId, requestId);
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("Only pending requests can be approved");
        }
        request.setStatus("APPROVED");
        request.setCurrentApproverUserId(null);
        request = approvalRequestRepository.save(request);
        insertHistory(request.getId(), "APPROVED", remarks);
        transitionEntityOnApproved(resolveEntity(organizationId, request.getEntityType(), request.getEntityId()));
        auditEventWriter.write(organizationId, request.getBranchId(), "APPROVAL_REQUEST_APPROVED", "approval_request", request.getId(), request.getEntityNumber(), "APPROVE", null, null, null, "Approval request approved", json("requestId", request.getId(), "remarks", remarks));
        return request;
    }

    public ApprovalRequest reject(Long organizationId, Long requestId, String remarks) {
        ApprovalRequest request = findOwnedRequest(organizationId, requestId);
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("Only pending requests can be rejected");
        }
        request.setStatus("REJECTED");
        request.setCurrentApproverUserId(null);
        request = approvalRequestRepository.save(request);
        insertHistory(request.getId(), "REJECTED", remarks);
        transitionEntityOnRejected(resolveEntity(organizationId, request.getEntityType(), request.getEntityId()), remarks);
        auditEventWriter.write(organizationId, request.getBranchId(), "APPROVAL_REQUEST_REJECTED", "approval_request", request.getId(), request.getEntityNumber(), "REJECT", null, null, null, "Approval request rejected", json("requestId", request.getId(), "remarks", remarks));
        return request;
    }

    public ApprovalRequest cancel(Long organizationId, Long requestId, String remarks) {
        ApprovalRequest request = findOwnedRequest(organizationId, requestId);
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("Only pending requests can be cancelled");
        }
        request.setStatus("CANCELLED");
        request.setCurrentApproverUserId(null);
        request = approvalRequestRepository.save(request);
        insertHistory(request.getId(), "CANCELLED", remarks);
        transitionEntityOnRejected(resolveEntity(organizationId, request.getEntityType(), request.getEntityId()), remarks);
        auditEventWriter.write(organizationId, request.getBranchId(), "APPROVAL_REQUEST_CANCELLED", "approval_request", request.getId(), request.getEntityNumber(), "CANCEL", null, null, null, "Approval request cancelled", json("requestId", request.getId(), "remarks", remarks));
        return request;
    }

    private ApprovalRequest findOwnedRequest(Long organizationId, Long requestId) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + requestId));
        if (!organizationId.equals(request.getOrganizationId())) {
            throw new BusinessException("Approval request does not belong to organization " + organizationId);
        }
        return request;
    }

    private void insertHistory(Long requestId, String action, String remarks) {
        ApprovalHistory history = new ApprovalHistory();
        history.setApprovalRequestId(requestId);
        history.setApproverUserId(ErpSecurityUtils.currentUserId().orElse(1L));
        history.setAction(action);
        history.setApproverRoleSnapshot("ERP_USER");
        history.setRemarks(remarks);
        history.setActionAt(LocalDateTime.now());
        approvalHistoryRepository.save(history);
    }

    private Optional<ApprovalRule> findMatchingRule(Long organizationId, EntityRef entityRef, String approvalType) {
        return approvalRuleRepository.findByOrganizationIdAndEntityTypeAndActiveTrueOrderByPriorityOrderAsc(organizationId, entityRef.entityType()).stream()
                .filter(rule -> rule.getBranchId() == null || entityRef.branchId() == null || rule.getBranchId().equals(entityRef.branchId()))
                .filter(rule -> approvalType == null || approvalType.isBlank() || approvalType.equalsIgnoreCase(rule.getApprovalType()))
                .filter(rule -> matchesAmount(rule, entityRef.amount()))
                .findFirst();
    }

    private boolean matchesAmount(ApprovalRule rule, BigDecimal amount) {
        if (amount == null) {
            return true;
        }
        if (rule.getMinAmount() != null && amount.compareTo(rule.getMinAmount()) < 0) {
            return false;
        }
        if (rule.getMaxAmount() != null && amount.compareTo(rule.getMaxAmount()) > 0) {
            return false;
        }
        return true;
    }

    private EntityRef resolveEntity(Long organizationId, String entityType, Long entityId) {
        return switch (entityType) {
            case "purchase_order" -> {
                PurchaseOrder po = purchaseOrderRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + entityId));
                if (!organizationId.equals(po.getOrganizationId())) throw new BusinessException("Entity does not belong to organization");
                yield new EntityRef(entityType, po.getId(), po.getPoNumber(), po.getSupplierId(), po.getBranchId(), po.getTotalAmount());
            }
            case "stock_adjustment" -> {
                StockAdjustment sa = stockAdjustmentRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Stock adjustment not found: " + entityId));
                if (!organizationId.equals(sa.getOrganizationId())) throw new BusinessException("Entity does not belong to organization");
                yield new EntityRef(entityType, sa.getId(), sa.getAdjustmentNumber(), null, sa.getBranchId(), null);
            }
            case "stock_transfer" -> {
                StockTransfer transfer = stockTransferRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found: " + entityId));
                if (!organizationId.equals(transfer.getOrganizationId())) throw new BusinessException("Entity does not belong to organization");
                yield new EntityRef(entityType, transfer.getId(), transfer.getTransferNumber(), null, transfer.getBranchId(), null);
            }
            case "sales_invoice" -> {
                SalesInvoice si = salesInvoiceRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + entityId));
                if (!organizationId.equals(si.getOrganizationId())) throw new BusinessException("Entity does not belong to organization");
                yield new EntityRef(entityType, si.getId(), si.getInvoiceNumber(), null, si.getBranchId(), si.getTotalAmount());
            }
            case "expense" -> {
                Expense expense = expenseRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + entityId));
                if (!organizationId.equals(expense.getOrganizationId())) throw new BusinessException("Entity does not belong to organization");
                yield new EntityRef(entityType, expense.getId(), expense.getExpenseNumber(), null, expense.getBranchId(), expense.getAmount());
            }
            case "sales_return" -> {
                SalesReturn salesReturn = salesReturnRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sales return not found: " + entityId));
                if (!organizationId.equals(salesReturn.getOrganizationId())) throw new BusinessException("Entity does not belong to organization");
                yield new EntityRef(entityType, salesReturn.getId(), salesReturn.getReturnNumber(), null, salesReturn.getBranchId(), salesReturn.getTotalAmount());
            }
            case "purchase_return" -> {
                PurchaseReturn purchaseReturn = purchaseReturnRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Purchase return not found: " + entityId));
                if (!organizationId.equals(purchaseReturn.getOrganizationId())) throw new BusinessException("Entity does not belong to organization");
                yield new EntityRef(entityType, purchaseReturn.getId(), purchaseReturn.getReturnNumber(), purchaseReturn.getSupplierId(), purchaseReturn.getBranchId(), purchaseReturn.getTotalAmount());
            }
            case "warranty_claim" -> {
                WarrantyClaim claim = warrantyClaimRepository.findByOrganizationIdAndId(organizationId, entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found: " + entityId));
                yield new EntityRef(entityType, claim.getId(), claim.getClaimNumber(), claim.getSupplierId(), claim.getBranchId(), BigDecimal.ZERO);
            }
            case "service_replacement" -> {
                ServiceReplacement replacement = serviceReplacementRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Service replacement not found: " + entityId));
                if (!organizationId.equals(replacement.getOrganizationId())) throw new BusinessException("Entity does not belong to organization");
                BigDecimal replacementAmount = accountingPostingService.estimateSalesCost(
                        replacement.getOrganizationId(),
                        replacement.getWarehouseId(),
                        replacement.getReplacementProductId(),
                        replacement.getReplacementBaseQuantity()
                );
                yield new EntityRef(entityType, replacement.getId(), replacement.getReplacementNumber(), null, replacement.getBranchId(), replacementAmount);
            }
            default -> throw new BusinessException("Unsupported approval entity type: " + entityType);
        };
    }

    private String resolveApproverRoleSnapshot(String requestedSnapshot, Role approverRole) {
        if (requestedSnapshot != null && !requestedSnapshot.isBlank()) {
            return requestedSnapshot;
        }
        if (approverRole == null) {
            return null;
        }
        return approverRole.getCode();
    }

    private void transitionEntityOnRequested(EntityRef ref) {
        switch (ref.entityType()) {
            case "purchase_order" -> purchaseOrderRepository.findById(ref.entityId()).ifPresent(po -> {
                po.setStatus("PENDING_APPROVAL");
                purchaseOrderRepository.save(po);
            });
            case "stock_adjustment" -> stockAdjustmentRepository.findById(ref.entityId()).ifPresent(sa -> {
                sa.setStatus("PENDING_APPROVAL");
                stockAdjustmentRepository.save(sa);
            });
            case "stock_transfer" -> stockTransferRepository.findById(ref.entityId()).ifPresent(transfer -> {
                transfer.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
                stockTransferRepository.save(transfer);
            });
            case "sales_invoice" -> salesInvoiceRepository.findById(ref.entityId()).ifPresent(si -> {
                si.setStatus("PENDING_APPROVAL");
                salesInvoiceRepository.save(si);
            });
            case "expense" -> expenseRepository.findById(ref.entityId()).ifPresent(expense -> {
                expense.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
                expenseRepository.save(expense);
            });
            case "service_replacement" -> serviceReplacementRepository.findById(ref.entityId()).ifPresent(replacement -> {
                replacement.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
                serviceReplacementRepository.save(replacement);
            });
            case "sales_return" -> salesReturnRepository.findById(ref.entityId()).ifPresent(salesReturn -> {
                salesReturn.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
                salesReturnRepository.save(salesReturn);
            });
            case "purchase_return" -> purchaseReturnRepository.findById(ref.entityId()).ifPresent(purchaseReturn -> {
                purchaseReturn.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
                purchaseReturnRepository.save(purchaseReturn);
            });
            case "warranty_claim" -> warrantyClaimRepository.findById(ref.entityId()).ifPresent(claim -> {
                claim.setStatus(ErpDocumentStatuses.SUBMITTED);
                warrantyClaimRepository.save(claim);
            });
            default -> { }
        }
    }

    private void transitionEntityOnApproved(EntityRef ref) {
        switch (ref.entityType()) {
            case "purchase_order" -> purchaseOrderRepository.findById(ref.entityId()).ifPresent(po -> {
                po.setStatus("APPROVED");
                po.setApprovedAt(LocalDateTime.now());
                po.setApprovedBy(ErpSecurityUtils.currentUserId().orElse(1L));
                purchaseOrderRepository.save(po);
            });
            case "stock_adjustment" -> stockAdjustmentRepository.findById(ref.entityId()).ifPresent(sa -> {
                stockAdjustmentPostingService.finalizeApprovedAdjustment(sa.getId());
            });
            case "stock_transfer" -> stockTransferRepository.findById(ref.entityId()).ifPresent(transfer -> {
                stockTransferPostingService.finalizeApprovedTransfer(transfer.getId());
            });
            case "sales_invoice" -> salesInvoiceRepository.findById(ref.entityId()).ifPresent(si -> {
                salesInvoicePostingService.finalizeApprovedInvoice(si.getId());
            });
            case "expense" -> expenseRepository.findById(ref.entityId()).ifPresent(expense -> {
                expense.setStatus(ErpDocumentStatuses.APPROVED);
                expense.setApprovedAt(LocalDateTime.now());
                expense.setApprovedBy(ErpSecurityUtils.currentUserId().orElse(1L));
                expenseRepository.save(expense);
                accountingPostingService.postExpenseAccrual(expense, resolveExpenseAccountId(expense));
            });
            case "service_replacement" -> serviceReplacementRepository.findById(ref.entityId()).ifPresent(replacement ->
                    serviceReplacementPostingService.finalizeApprovedReplacement(replacement.getId()));
            case "sales_return" -> salesReturnRepository.findById(ref.entityId()).ifPresent(salesReturn ->
                    returnPostingService.finalizeApprovedSalesReturn(salesReturn.getId()));
            case "purchase_return" -> purchaseReturnRepository.findById(ref.entityId()).ifPresent(purchaseReturn ->
                    returnPostingService.finalizeApprovedPurchaseReturn(purchaseReturn.getId()));
            case "warranty_claim" -> warrantyClaimRepository.findById(ref.entityId()).ifPresent(claim -> {
                claim.setStatus(ErpDocumentStatuses.APPROVED);
                if (claim.getApprovedOn() == null) {
                    claim.setApprovedOn(java.time.LocalDate.now());
                }
                warrantyClaimRepository.save(claim);
            });
            default -> { }
        }
    }

    private void transitionEntityOnRejected(EntityRef ref, String remarks) {
        switch (ref.entityType()) {
            case "purchase_order" -> purchaseOrderRepository.findById(ref.entityId()).ifPresent(po -> {
                po.setStatus("REJECTED");
                po.setCancelReason(remarks);
                po.setCancelledAt(LocalDateTime.now());
                po.setCancelledBy(ErpSecurityUtils.currentUserId().orElse(1L));
                purchaseOrderRepository.save(po);
            });
            case "stock_adjustment" -> stockAdjustmentRepository.findById(ref.entityId()).ifPresent(sa -> {
                sa.setStatus("REJECTED");
                stockAdjustmentRepository.save(sa);
            });
            case "stock_transfer" -> stockTransferRepository.findById(ref.entityId()).ifPresent(transfer -> {
                transfer.setStatus(ErpDocumentStatuses.REJECTED);
                stockTransferRepository.save(transfer);
            });
            case "sales_invoice" -> salesInvoiceRepository.findById(ref.entityId()).ifPresent(si -> {
                si.setStatus("REJECTED");
                si.setCancelReason(remarks);
                si.setCancelledAt(LocalDateTime.now());
                si.setCancelledBy(ErpSecurityUtils.currentUserId().orElse(1L));
                salesInvoiceRepository.save(si);
                inventoryReservationService.releaseSalesInvoiceReservations(si.getOrganizationId(), si.getId());
            });
            case "expense" -> expenseRepository.findById(ref.entityId()).ifPresent(expense -> {
                expense.setStatus(ErpDocumentStatuses.REJECTED);
                expense.setCancelReason(remarks);
                expense.setCancelledAt(LocalDateTime.now());
                expense.setCancelledBy(ErpSecurityUtils.currentUserId().orElse(1L));
                expenseRepository.save(expense);
            });
            case "service_replacement" -> serviceReplacementRepository.findById(ref.entityId()).ifPresent(replacement -> {
                replacement.setStatus(ErpDocumentStatuses.REJECTED);
                serviceReplacementRepository.save(replacement);
            });
            case "sales_return" -> salesReturnRepository.findById(ref.entityId()).ifPresent(salesReturn -> {
                salesReturn.setStatus(ErpDocumentStatuses.REJECTED);
                salesReturnRepository.save(salesReturn);
            });
            case "purchase_return" -> purchaseReturnRepository.findById(ref.entityId()).ifPresent(purchaseReturn -> {
                purchaseReturn.setStatus(ErpDocumentStatuses.REJECTED);
                purchaseReturnRepository.save(purchaseReturn);
            });
            case "warranty_claim" -> warrantyClaimRepository.findById(ref.entityId()).ifPresent(claim -> {
                claim.setStatus("OPEN");
                warrantyClaimRepository.save(claim);
            });
            default -> { }
        }
    }

    private Long resolveExpenseAccountId(Expense expense) {
        ExpenseCategory category = expenseCategoryRepository.findByIdAndOrganizationId(expense.getExpenseCategoryId(), expense.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found: " + expense.getExpenseCategoryId()));
        if (category.getExpenseAccountId() != null) {
            return category.getExpenseAccountId();
        }
        return accountRepository.findByOrganizationIdAndCode(expense.getOrganizationId(), "EXPENSE_CONTROL")
                .map(account -> account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Default expense account not configured for expense " + expense.getExpenseNumber()));
    }

    private String json(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append('"').append(kv[i]).append('"').append(':');
            Object v = kv[i + 1];
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append('"').append(String.valueOf(v).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public record ApprovalRequestDetails(ApprovalRequest request, List<ApprovalHistory> history) {}
    public record ApprovalEvaluation(boolean approvalRequired, String entityType, Long entityId, String entityNumber,
                                     String approvalType, Long branchId, BigDecimal entityAmount, Long matchedRuleId,
                                     Long approverRoleId, String approverRoleCode, String approverRoleName,
                                     boolean pendingRequestExists, Long pendingRequestId) {}
    public record ApprovalQueueSummary(long totalPending, List<ApprovalQueueSummaryItem> pendingByEntityType) {}
    public record ApprovalQueueSummaryItem(String entityType, long pendingCount) {}
    private record EntityRef(String entityType, Long entityId, String entityNumber, Long supplierId, Long branchId, BigDecimal amount) {}
}
