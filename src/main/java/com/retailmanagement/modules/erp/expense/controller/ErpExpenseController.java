package com.retailmanagement.modules.erp.expense.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.expense.dto.ErpExpenseDtos;
import com.retailmanagement.modules.erp.expense.dto.ErpExpenseResponses;
import com.retailmanagement.modules.erp.expense.service.ErpExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/expenses")
@RequiredArgsConstructor
@Tag(name = "ERP Expenses", description = "ERP expense category and expense posting endpoints")
public class ErpExpenseController {

    private final ErpExpenseService erpExpenseService;

    @GetMapping("/categories")
    @Operation(summary = "List expense categories")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<List<ErpExpenseResponses.ExpenseCategoryResponse>> listCategories(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpExpenseService.listCategories(orgId));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create expense category")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<ErpExpenseResponses.ExpenseCategoryResponse> createCategory(@RequestBody @Valid ErpExpenseDtos.CreateExpenseCategoryRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpExpenseService.createCategory(orgId, request), "Expense category created");
    }

    @GetMapping
    @Operation(summary = "List expenses")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<List<ErpExpenseResponses.ExpenseResponse>> listExpenses(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpExpenseService.listExpenses(orgId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense by id")
    @PreAuthorize("hasAuthority('accounting.view')")
    public ErpApiResponse<ErpExpenseResponses.ExpenseResponse> getExpense(@PathVariable Long id) {
        return ErpApiResponse.ok(erpExpenseService.getExpense(id));
    }

    @PostMapping
    @Operation(summary = "Create expense")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<ErpExpenseResponses.ExpenseResponse> createExpense(@RequestBody @Valid ErpExpenseDtos.CreateExpenseRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpExpenseService.createExpense(orgId, branchId, request), "Expense created");
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay expense")
    @PreAuthorize("hasAuthority('accounting.post')")
    public ErpApiResponse<ErpExpenseResponses.ExpenseResponse> payExpense(@PathVariable Long id,
                                                                          @RequestBody @Valid ErpExpenseDtos.PayExpenseRequest request) {
        return ErpApiResponse.ok(erpExpenseService.payExpense(id, request), "Expense paid");
    }
}
