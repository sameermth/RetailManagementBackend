package com.retailmanagement.modules.erp.expense.repository;

import com.retailmanagement.modules.erp.expense.entity.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository("erpExpenseRepository")
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findTop100ByOrganizationIdOrderByExpenseDateDescIdDesc(Long organizationId);
    List<Expense> findByOrganizationIdAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(Long organizationId, LocalDate fromDate, LocalDate toDate);
    Optional<Expense> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from ErpExpense e
            where e.expenseDate between :startDate and :endDate
              and e.status <> 'CANCELLED'
            """)
    BigDecimal getTotalExpensesForPeriod(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from ErpExpense e
            where e.organizationId = :organizationId
              and e.expenseDate between :startDate and :endDate
              and e.status <> 'CANCELLED'
            """)
    BigDecimal getTotalExpensesForPeriod(@Param("organizationId") Long organizationId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("""
            select c.name, coalesce(sum(e.amount), 0)
            from ErpExpense e
            join ErpExpenseCategory c on c.id = e.expenseCategoryId
            where e.expenseDate between :startDate and :endDate
              and e.status <> 'CANCELLED'
            group by c.name
            order by coalesce(sum(e.amount), 0) desc
            """)
    List<Object[]> getExpensesGroupedByCategory(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("""
            select c.id, c.code, c.name, coalesce(sum(e.amount), 0), count(e.id)
            from ErpExpense e
            join ErpExpenseCategory c on c.id = e.expenseCategoryId
            where e.organizationId = :organizationId
              and e.expenseDate between :startDate and :endDate
              and e.status <> 'CANCELLED'
            group by c.id, c.code, c.name
            order by coalesce(sum(e.amount), 0) desc
            """)
    List<Object[]> getExpensesGroupedByCategory(@Param("organizationId") Long organizationId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from ErpExpense e
            where e.organizationId = :organizationId
              and e.expenseDate between :startDate and :endDate
              and e.status = :status
            """)
    BigDecimal getTotalExpensesForPeriodByStatus(@Param("organizationId") Long organizationId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("status") String status);
}
