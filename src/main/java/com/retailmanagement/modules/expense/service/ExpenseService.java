package com.retailmanagement.modules.expense.service;

import com.retailmanagement.modules.expense.dto.request.ExpenseApprovalRequest;
import com.retailmanagement.modules.expense.dto.request.ExpenseRequest;
import com.retailmanagement.modules.expense.dto.response.ExpenseResponse;
import com.retailmanagement.modules.expense.dto.response.ExpenseSummaryResponse;
import com.retailmanagement.modules.expense.enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseService {

    ExpenseResponse createExpense(ExpenseRequest request);

    ExpenseResponse updateExpense(Long id, ExpenseRequest request);

    ExpenseResponse getExpenseById(Long id);

    ExpenseResponse getExpenseByNumber(String expenseNumber);

    Page<ExpenseResponse> getAllExpenses(Pageable pageable);

    List<ExpenseResponse> getExpensesByCategory(Long categoryId);

    Page<ExpenseResponse> getExpensesByCategory(Long categoryId, Pageable pageable);

    List<ExpenseResponse> getExpensesByUser(Long userId);

    Page<ExpenseResponse> getExpensesByUser(Long userId, Pageable pageable);

    List<ExpenseResponse> getExpensesByStatus(ExpenseStatus status);

    Page<ExpenseResponse> getExpensesByStatus(ExpenseStatus status, Pageable pageable);

    List<ExpenseResponse> getExpensesByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<ExpenseResponse> getExpensesByVendor(String vendor);

    ExpenseResponse approveExpense(Long id, ExpenseApprovalRequest request);

    ExpenseResponse rejectExpense(Long id, String reason);

    ExpenseResponse markAsPaid(Long id);

    void cancelExpense(Long id, String reason);

    ExpenseResponse uploadReceipt(Long id, String receiptUrl);

    BigDecimal getTotalExpenses(LocalDateTime startDate, LocalDateTime endDate);

    ExpenseSummaryResponse getExpenseSummary(String period);

    Long getPendingApprovalCount();

    List<ExpenseResponse> getRecentExpenses(int limit);

    boolean isExpenseNumberUnique(String expenseNumber);
}