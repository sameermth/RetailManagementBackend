package com.retailmanagement.modules.expense.repository;

import com.retailmanagement.modules.expense.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    Optional<ExpenseCategory> findByCategoryCode(String categoryCode);

    Optional<ExpenseCategory> findByName(String name);

    List<ExpenseCategory> findByParentCategoryIsNull();

    List<ExpenseCategory> findByParentCategoryId(Long parentId);

    List<ExpenseCategory> findByType(String type);

    @Query("SELECT c FROM ExpenseCategory c WHERE c.isActive = true")
    List<ExpenseCategory> findAllActive();

    @Query("SELECT c FROM ExpenseCategory c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ExpenseCategory> searchCategories(@Param("searchTerm") String searchTerm);

    boolean existsByName(String name);

    boolean existsByCategoryCode(String categoryCode);
}