package com.retailmanagement.modules.expense.repository;

import com.retailmanagement.modules.expense.model.RecurringExpense;
import com.retailmanagement.modules.expense.enums.RecurringFrequency;
import com.retailmanagement.modules.expense.enums.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    Optional<RecurringExpense> findByRecurringExpenseNumber(String recurringExpenseNumber);

    List<RecurringExpense> findByCategoryId(Long categoryId);

    List<RecurringExpense> findByFrequency(RecurringFrequency frequency);

    List<RecurringExpense> findByStatus(ExpenseStatus status);

    @Query("SELECT r FROM RecurringExpense r WHERE r.isActive = true AND r.nextGenerationDate <= :date")
    List<RecurringExpense> findRecurringExpensesDueForGeneration(@Param("date") LocalDate date);

    @Query("SELECT r FROM RecurringExpense r WHERE r.endDate < :date AND r.status != 'COMPLETED'")
    List<RecurringExpense> findExpiredRecurringExpenses(@Param("date") LocalDate date);

    boolean existsByRecurringExpenseNumber(String recurringExpenseNumber);
}