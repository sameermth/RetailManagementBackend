package com.retailmanagement.modules.expense.service;

import com.retailmanagement.modules.expense.dto.request.RecurringExpenseRequest;
import com.retailmanagement.modules.expense.dto.response.RecurringExpenseResponse;
import com.retailmanagement.modules.expense.enums.RecurringFrequency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RecurringExpenseService {

    RecurringExpenseResponse createRecurringExpense(RecurringExpenseRequest request);

    RecurringExpenseResponse updateRecurringExpense(Long id, RecurringExpenseRequest request);

    RecurringExpenseResponse getRecurringExpenseById(Long id);

    RecurringExpenseResponse getRecurringExpenseByNumber(String recurringExpenseNumber);

    Page<RecurringExpenseResponse> getAllRecurringExpenses(Pageable pageable);

    List<RecurringExpenseResponse> getRecurringExpensesByCategory(Long categoryId);

    List<RecurringExpenseResponse> getRecurringExpensesByFrequency(RecurringFrequency frequency);

    void deleteRecurringExpense(Long id);

    void activateRecurringExpense(Long id);

    void deactivateRecurringExpense(Long id);

    void generateRecurringExpenses();

    int generateExpensesForRecurring(Long id);

    List<RecurringExpenseResponse> getRecurringExpensesDueForGeneration();

    boolean isRecurringExpenseNumberUnique(String recurringExpenseNumber);
}