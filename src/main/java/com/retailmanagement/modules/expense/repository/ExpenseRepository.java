package com.retailmanagement.modules.expense.repository;

import com.retailmanagement.modules.expense.model.Expense;
import com.retailmanagement.modules.expense.enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByExpenseNumber(String expenseNumber);

    List<Expense> findByCategoryId(Long categoryId);

    Page<Expense> findByCategoryId(Long categoryId, Pageable pageable);

    List<Expense> findByUserId(Long userId);

    Page<Expense> findByUserId(Long userId, Pageable pageable);

    List<Expense> findByStatus(ExpenseStatus status);

    Page<Expense> findByStatus(ExpenseStatus status, Pageable pageable);

    List<Expense> findByExpenseDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT e FROM Expense e WHERE e.vendor LIKE %:vendor%")
    List<Expense> findByVendor(@Param("vendor") String vendor);

    @Query("SELECT e FROM Expense e WHERE e.isReimbursable = true AND e.status = 'PAID'")
    List<Expense> findReimbursableExpenses();

    @Query("SELECT e FROM Expense e WHERE e.isBillable = true AND e.customerId = :customerId")
    List<Expense> findBillableExpensesByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesForPeriod(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.category.id = :categoryId " +
            "AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesByCategoryForPeriod(@Param("categoryId") Long categoryId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e.category.name, SUM(e.amount) FROM Expense e " +
            "WHERE e.expenseDate BETWEEN :startDate AND :endDate " +
            "GROUP BY e.category.name")
    List<Object[]> getExpensesGroupedByCategory(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e.vendor, SUM(e.amount), COUNT(e) FROM Expense e " +
            "WHERE e.vendor IS NOT NULL AND e.expenseDate BETWEEN :startDate AND :endDate " +
            "GROUP BY e.vendor ORDER BY SUM(e.amount) DESC")
    List<Object[]> getTopVendors(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate,
                                 Pageable pageable);

    boolean existsByExpenseNumber(String expenseNumber);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.status = :status AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesByStatusForPeriod(@Param("status") ExpenseStatus status,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
}