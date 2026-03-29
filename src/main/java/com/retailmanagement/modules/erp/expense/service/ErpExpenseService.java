package com.retailmanagement.modules.erp.expense.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.approval.service.ErpApprovalService;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.expense.dto.ErpExpenseDtos;
import com.retailmanagement.modules.erp.expense.dto.ErpExpenseResponses;
import com.retailmanagement.modules.erp.expense.entity.Expense;
import com.retailmanagement.modules.erp.expense.entity.ExpenseCategory;
import com.retailmanagement.modules.erp.expense.repository.ExpenseCategoryRepository;
import com.retailmanagement.modules.erp.expense.repository.ExpenseRepository;
import com.retailmanagement.modules.erp.finance.entity.Account;
import com.retailmanagement.modules.erp.finance.repository.AccountRepository;
import com.retailmanagement.modules.erp.finance.service.ErpAccountingPostingService;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ErpExpenseService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final AccountRepository accountRepository;
    private final ErpAccessGuard accessGuard;
    private final SubscriptionAccessService subscriptionAccessService;
    private final ErpAccountingPostingService accountingPostingService;
    private final ErpApprovalService erpApprovalService;
    private final AuditEventWriter auditEventWriter;

    @Transactional(readOnly = true)
    public List<ErpExpenseResponses.ExpenseCategoryResponse> listCategories(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "expenses");
        return expenseCategoryRepository.findByOrganizationIdAndIsActiveTrueOrderByCodeAsc(organizationId).stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    public ErpExpenseResponses.ExpenseCategoryResponse createCategory(Long organizationId, ErpExpenseDtos.CreateExpenseCategoryRequest request) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "expenses");
        expenseCategoryRepository.findByOrganizationIdAndCode(organizationId, request.code())
                .ifPresent(existing -> {
                    throw new BusinessException("Expense category code already exists: " + request.code());
                });

        Long accountId = request.expenseAccountId() != null
                ? validateExpenseAccount(organizationId, request.expenseAccountId()).getId()
                : defaultExpenseAccount(organizationId).getId();

        ExpenseCategory category = new ExpenseCategory();
        category.setOrganizationId(organizationId);
        category.setCode(request.code().trim().toUpperCase());
        category.setName(request.name().trim());
        category.setExpenseAccountId(accountId);
        category.setIsActive(Boolean.TRUE);
        category = expenseCategoryRepository.save(category);

        auditEventWriter.write(
                organizationId,
                null,
                "EXPENSE_CATEGORY_CREATED",
                "expense_category",
                category.getId(),
                category.getCode(),
                "CREATE",
                null,
                null,
                null,
                "Expense category created",
                ErpJsonPayloads.of("code", category.getCode(), "name", category.getName())
        );

        return toCategoryResponse(category);
    }

    @Transactional(readOnly = true)
    public List<ErpExpenseResponses.ExpenseResponse> listExpenses(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "expenses");
        return expenseRepository.findTop100ByOrganizationIdOrderByExpenseDateDescIdDesc(organizationId).stream()
                .map(this::toExpenseResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ErpExpenseResponses.ExpenseResponse getExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));
        accessGuard.assertOrganizationAccess(expense.getOrganizationId());
        accessGuard.assertBranchAccess(expense.getOrganizationId(), expense.getBranchId());
        subscriptionAccessService.assertFeature(expense.getOrganizationId(), "expenses");
        return toExpenseResponse(expense);
    }

    public ErpExpenseResponses.ExpenseResponse createExpense(Long organizationId, Long branchId, ErpExpenseDtos.CreateExpenseRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "expenses");
        ExpenseCategory category = expenseCategoryRepository.findByIdAndOrganizationId(request.expenseCategoryId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found: " + request.expenseCategoryId()));

        Expense expense = new Expense();
        expense.setOrganizationId(organizationId);
        expense.setBranchId(branchId);
        expense.setExpenseCategoryId(category.getId());
        expense.setExpenseNumber("EXP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis());
        expense.setExpenseDate(request.expenseDate() == null ? LocalDate.now() : request.expenseDate());
        expense.setDueDate(request.dueDate() == null ? expense.getExpenseDate() : request.dueDate());
        expense.setAmount(request.amount());
        expense.setReceiptUrl(request.receiptUrl());
        expense.setRemarks(request.remarks());
        expense.setSubmittedAt(LocalDateTime.now());
        expense.setSubmittedBy(ErpSecurityUtils.currentUserId().orElse(1L));

        boolean markPaid = Boolean.TRUE.equals(request.markPaid());
        expense.setStatus(ErpDocumentStatuses.SUBMITTED);

        expense = expenseRepository.save(expense);

        ErpApprovalService.ApprovalEvaluation evaluation = erpApprovalService.evaluate(
                organizationId,
                new com.retailmanagement.modules.erp.approval.dto.ErpApprovalDtos.ApprovalEvaluationQuery(
                        "expense",
                        expense.getId(),
                        "EXPENSE_CREATE"
                )
        );

        if (evaluation.approvalRequired()) {
            expense.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
            expense = expenseRepository.save(expense);
            if (!evaluation.pendingRequestExists()) {
                erpApprovalService.createRequest(
                        organizationId,
                        branchId,
                        new com.retailmanagement.modules.erp.approval.dto.ErpApprovalDtos.CreateApprovalRequest(
                                "expense",
                                expense.getId(),
                                expense.getExpenseNumber(),
                                "EXPENSE_CREATE",
                                "Expense amount matched approval rule",
                                null,
                                null
                        )
                );
            }
        } else {
            Account expenseAccount = resolveExpenseAccount(organizationId, category);
            expense.setApprovedAt(LocalDateTime.now());
            expense.setApprovedBy(ErpSecurityUtils.currentUserId().orElse(1L));
            if (markPaid) {
                String paymentMethod = normalizePaymentMethod(request.paymentMethod());
                expense.setPaymentMethod(paymentMethod);
                expense.setStatus(ErpDocumentStatuses.PAID);
                expense.setPaidAt(LocalDateTime.now());
                expense = expenseRepository.save(expense);
                accountingPostingService.postExpensePaid(expense, expenseAccount.getId());
            } else {
                expense.setStatus(ErpDocumentStatuses.APPROVED);
                expense = expenseRepository.save(expense);
                accountingPostingService.postExpenseAccrual(expense, expenseAccount.getId());
            }
        }

        auditEventWriter.write(
                organizationId,
                branchId,
                "EXPENSE_CREATED",
                "expense",
                expense.getId(),
                expense.getExpenseNumber(),
                "CREATE",
                null,
                null,
                null,
                "Expense created",
                ErpJsonPayloads.of("expenseNumber", expense.getExpenseNumber(), "amount", expense.getAmount(), "status", expense.getStatus())
        );

        return toExpenseResponse(expense);
    }

    public ErpExpenseResponses.ExpenseResponse payExpense(Long id, ErpExpenseDtos.PayExpenseRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));
        accessGuard.assertOrganizationAccess(expense.getOrganizationId());
        accessGuard.assertBranchAccess(expense.getOrganizationId(), expense.getBranchId());
        subscriptionAccessService.assertFeature(expense.getOrganizationId(), "expenses");
        if (ErpDocumentStatuses.CANCELLED.equals(expense.getStatus())) {
            throw new BusinessException("Cannot pay a cancelled expense");
        }
        if (ErpDocumentStatuses.PENDING_APPROVAL.equals(expense.getStatus())) {
            throw new BusinessException("Cannot pay an expense pending approval");
        }
        if (ErpDocumentStatuses.REJECTED.equals(expense.getStatus())) {
            throw new BusinessException("Cannot pay a rejected expense");
        }
        if (ErpDocumentStatuses.PAID.equals(expense.getStatus())) {
            throw new BusinessException("Expense is already paid");
        }

        expense.setPaymentMethod(normalizePaymentMethod(request.paymentMethod()));
        expense.setPaidAt((request.paidDate() == null ? LocalDate.now() : request.paidDate()).atStartOfDay());
        expense.setStatus(ErpDocumentStatuses.PAID);
        if (request.remarks() != null && !request.remarks().isBlank()) {
            expense.setRemarks(request.remarks());
        }
        expense = expenseRepository.save(expense);

        accountingPostingService.settleExpensePayment(expense);

        auditEventWriter.write(
                expense.getOrganizationId(),
                expense.getBranchId(),
                "EXPENSE_PAID",
                "expense",
                expense.getId(),
                expense.getExpenseNumber(),
                "PAY",
                null,
                null,
                null,
                "Expense paid",
                ErpJsonPayloads.of("expenseNumber", expense.getExpenseNumber(), "amount", expense.getAmount(), "paymentMethod", expense.getPaymentMethod())
        );

        return toExpenseResponse(expense);
    }

    private Account validateExpenseAccount(Long organizationId, Long accountId) {
        Account account = accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense account not found: " + accountId));
        if (!"EXPENSE".equals(account.getAccountType())) {
            throw new BusinessException("Expense category account must be an EXPENSE account");
        }
        return account;
    }

    private Account defaultExpenseAccount(Long organizationId) {
        return accountRepository.findByOrganizationIdAndCode(organizationId, "EXPENSE_CONTROL")
                .orElseThrow(() -> new ResourceNotFoundException("Default expense account not configured"));
    }

    private Account resolveExpenseAccount(Long organizationId, ExpenseCategory category) {
        if (category.getExpenseAccountId() != null) {
            return validateExpenseAccount(organizationId, category.getExpenseAccountId());
        }
        return defaultExpenseAccount(organizationId);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return "CASH";
        }
        return paymentMethod.trim().toUpperCase();
    }

    private ErpExpenseResponses.ExpenseCategoryResponse toCategoryResponse(ExpenseCategory category) {
        return new ErpExpenseResponses.ExpenseCategoryResponse(
                category.getId(),
                category.getOrganizationId(),
                category.getCode(),
                category.getName(),
                category.getExpenseAccountId(),
                category.getIsActive()
        );
    }

    private ErpExpenseResponses.ExpenseResponse toExpenseResponse(Expense expense) {
        BigDecimal outstanding = ErpDocumentStatuses.PAID.equals(expense.getStatus()) ? BigDecimal.ZERO : expense.getAmount();
        return new ErpExpenseResponses.ExpenseResponse(
                expense.getId(),
                expense.getOrganizationId(),
                expense.getBranchId(),
                expense.getExpenseCategoryId(),
                expense.getExpenseNumber(),
                expense.getExpenseDate(),
                expense.getDueDate(),
                expense.getAmount(),
                outstanding,
                expense.getStatus(),
                expense.getPaymentMethod(),
                expense.getReceiptUrl(),
                expense.getRemarks()
        );
    }
}
