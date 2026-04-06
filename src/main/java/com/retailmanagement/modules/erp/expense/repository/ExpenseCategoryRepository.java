package com.retailmanagement.modules.erp.expense.repository;

import com.retailmanagement.modules.erp.expense.entity.ExpenseCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("erpExpenseCategoryRepository")
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    List<ExpenseCategory> findByOrganizationIdAndIsActiveTrueOrderByCodeAsc(Long organizationId);
    Optional<ExpenseCategory> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<ExpenseCategory> findByOrganizationIdAndCode(Long organizationId, String code);
    boolean existsByOrganizationIdAndExpenseAccountId(Long organizationId, Long expenseAccountId);
}
