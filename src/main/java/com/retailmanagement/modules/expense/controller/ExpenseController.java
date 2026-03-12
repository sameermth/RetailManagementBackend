package com.retailmanagement.modules.expense.controller;

import com.retailmanagement.modules.expense.dto.request.ExpenseApprovalRequest;
import com.retailmanagement.modules.expense.dto.request.ExpenseRequest;
import com.retailmanagement.modules.expense.dto.response.ExpenseResponse;
import com.retailmanagement.modules.expense.dto.response.ExpenseSummaryResponse;
import com.retailmanagement.modules.expense.enums.ExpenseStatus;
import com.retailmanagement.modules.expense.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Expense management endpoints")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT') or hasRole('EMPLOYEE')")
    @Operation(summary = "Create a new expense")
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {
        return new ResponseEntity<>(expenseService.createExpense(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Update an existing expense")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @GetMapping("/number/{expenseNumber}")
    @Operation(summary = "Get expense by number")
    public ResponseEntity<ExpenseResponse> getExpenseByNumber(@PathVariable String expenseNumber) {
        return ResponseEntity.ok(expenseService.getExpenseByNumber(expenseNumber));
    }

    @GetMapping
    @Operation(summary = "Get all expenses with pagination")
    public ResponseEntity<Page<ExpenseResponse>> getAllExpenses(
            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(expenseService.getAllExpenses(pageable));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get expenses by category")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(expenseService.getExpensesByCategory(categoryId));
    }

    @GetMapping("/category/{categoryId}/paged")
    @Operation(summary = "Get expenses by category with pagination")
    public ResponseEntity<Page<ExpenseResponse>> getExpensesByCategoryPaged(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(expenseService.getExpensesByCategory(categoryId, pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get expenses by user")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(expenseService.getExpensesByUser(userId));
    }

    @GetMapping("/user/{userId}/paged")
    @Operation(summary = "Get expenses by user with pagination")
    public ResponseEntity<Page<ExpenseResponse>> getExpensesByUserPaged(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(expenseService.getExpensesByUser(userId, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get expenses by status")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByStatus(@PathVariable ExpenseStatus status) {
        return ResponseEntity.ok(expenseService.getExpensesByStatus(status));
    }

    @GetMapping("/status/{status}/paged")
    @Operation(summary = "Get expenses by status with pagination")
    public ResponseEntity<Page<ExpenseResponse>> getExpensesByStatusPaged(
            @PathVariable ExpenseStatus status,
            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(expenseService.getExpensesByStatus(status, pageable));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get expenses by date range")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(expenseService.getExpensesByDateRange(startDate, endDate));
    }

    @GetMapping("/vendor")
    @Operation(summary = "Get expenses by vendor")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByVendor(@RequestParam String vendor) {
        return ResponseEntity.ok(expenseService.getExpensesByVendor(vendor));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT') or hasRole('MANAGER')")
    @Operation(summary = "Approve or reject an expense")
    public ResponseEntity<ExpenseResponse> approveExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseApprovalRequest request) {
        return ResponseEntity.ok(expenseService.approveExpense(id, request));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT') or hasRole('MANAGER')")
    @Operation(summary = "Reject an expense")
    public ResponseEntity<ExpenseResponse> rejectExpense(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(expenseService.rejectExpense(id, reason));
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Mark expense as paid")
    public ResponseEntity<ExpenseResponse> markAsPaid(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.markAsPaid(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Cancel an expense")
    public ResponseEntity<Void> cancelExpense(@PathVariable Long id, @RequestParam String reason) {
        expenseService.cancelExpense(id, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/upload-receipt")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT') or hasRole('EMPLOYEE')")
    @Operation(summary = "Upload receipt for expense")
    public ResponseEntity<ExpenseResponse> uploadReceipt(
            @PathVariable Long id,
            @RequestParam String receiptUrl) {
        return ResponseEntity.ok(expenseService.uploadReceipt(id, receiptUrl));
    }

    @GetMapping("/stats/total")
    @Operation(summary = "Get total expenses for period")
    public ResponseEntity<BigDecimal> getTotalExpenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(expenseService.getTotalExpenses(startDate, endDate));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get expense summary")
    public ResponseEntity<ExpenseSummaryResponse> getExpenseSummary(
            @RequestParam(defaultValue = "MONTH") String period) {
        return ResponseEntity.ok(expenseService.getExpenseSummary(period));
    }

    @GetMapping("/stats/pending-approval")
    @Operation(summary = "Get count of pending approval expenses")
    public ResponseEntity<Long> getPendingApprovalCount() {
        return ResponseEntity.ok(expenseService.getPendingApprovalCount());
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent expenses")
    public ResponseEntity<List<ExpenseResponse>> getRecentExpenses(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(expenseService.getRecentExpenses(limit));
    }

    @GetMapping("/check-expense-number")
    @Operation(summary = "Check if expense number is unique")
    public ResponseEntity<Boolean> checkExpenseNumberUnique(@RequestParam String expenseNumber) {
        return ResponseEntity.ok(expenseService.isExpenseNumberUnique(expenseNumber));
    }
}