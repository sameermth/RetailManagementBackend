package com.retailmanagement.modules.expense.controller;

import com.retailmanagement.modules.expense.dto.request.RecurringExpenseRequest;
import com.retailmanagement.modules.expense.dto.response.RecurringExpenseResponse;
import com.retailmanagement.modules.expense.enums.RecurringFrequency;
import com.retailmanagement.modules.expense.service.RecurringExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-expenses")
@RequiredArgsConstructor
@Tag(name = "Recurring Expenses", description = "Recurring expense management endpoints")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Create a new recurring expense")
    public ResponseEntity<RecurringExpenseResponse> createRecurringExpense(@Valid @RequestBody RecurringExpenseRequest request) {
        return new ResponseEntity<>(recurringExpenseService.createRecurringExpense(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Update an existing recurring expense")
    public ResponseEntity<RecurringExpenseResponse> updateRecurringExpense(
            @PathVariable Long id,
            @Valid @RequestBody RecurringExpenseRequest request) {
        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpense(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recurring expense by ID")
    public ResponseEntity<RecurringExpenseResponse> getRecurringExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpenseById(id));
    }

    @GetMapping("/number/{recurringExpenseNumber}")
    @Operation(summary = "Get recurring expense by number")
    public ResponseEntity<RecurringExpenseResponse> getRecurringExpenseByNumber(@PathVariable String recurringExpenseNumber) {
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpenseByNumber(recurringExpenseNumber));
    }

    @GetMapping
    @Operation(summary = "Get all recurring expenses with pagination")
    public ResponseEntity<Page<RecurringExpenseResponse>> getAllRecurringExpenses(
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(recurringExpenseService.getAllRecurringExpenses(pageable));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get recurring expenses by category")
    public ResponseEntity<List<RecurringExpenseResponse>> getRecurringExpensesByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpensesByCategory(categoryId));
    }

    @GetMapping("/frequency/{frequency}")
    @Operation(summary = "Get recurring expenses by frequency")
    public ResponseEntity<List<RecurringExpenseResponse>> getRecurringExpensesByFrequency(@PathVariable RecurringFrequency frequency) {
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpensesByFrequency(frequency));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a recurring expense")
    public ResponseEntity<Void> deleteRecurringExpense(@PathVariable Long id) {
        recurringExpenseService.deleteRecurringExpense(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Activate a recurring expense")
    public ResponseEntity<Void> activateRecurringExpense(@PathVariable Long id) {
        recurringExpenseService.activateRecurringExpense(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Deactivate a recurring expense")
    public ResponseEntity<Void> deactivateRecurringExpense(@PathVariable Long id) {
        recurringExpenseService.deactivateRecurringExpense(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/generate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Generate expenses for a recurring expense")
    public ResponseEntity<Integer> generateExpensesForRecurring(@PathVariable Long id) {
        return ResponseEntity.ok(recurringExpenseService.generateExpensesForRecurring(id));
    }

    @GetMapping("/due-for-generation")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get recurring expenses due for generation")
    public ResponseEntity<List<RecurringExpenseResponse>> getRecurringExpensesDueForGeneration() {
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpensesDueForGeneration());
    }

    @GetMapping("/check-number")
    @Operation(summary = "Check if recurring expense number is unique")
    public ResponseEntity<Boolean> checkRecurringExpenseNumberUnique(@RequestParam String recurringExpenseNumber) {
        return ResponseEntity.ok(recurringExpenseService.isRecurringExpenseNumberUnique(recurringExpenseNumber));
    }
}