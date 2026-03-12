package com.retailmanagement.modules.expense.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.expense.dto.request.ExpenseCategoryRequest;
import com.retailmanagement.modules.expense.dto.response.ExpenseCategoryResponse;
import com.retailmanagement.modules.expense.mapper.ExpenseCategoryMapper;
import com.retailmanagement.modules.expense.model.ExpenseCategory;
import com.retailmanagement.modules.expense.repository.ExpenseCategoryRepository;
import com.retailmanagement.modules.expense.repository.ExpenseRepository;
import com.retailmanagement.modules.expense.service.ExpenseCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryMapper categoryMapper;

    @Override
    public ExpenseCategoryResponse createCategory(ExpenseCategoryRequest request) {
        log.info("Creating new expense category with name: {}", request.getName());

        // Check if category name already exists
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Expense category with name " + request.getName() + " already exists");
        }

        ExpenseCategory category = categoryMapper.toEntity(request);

        // Generate category code
        category.setCategoryCode(generateCategoryCode());
        category.setAllocatedAmount(BigDecimal.ZERO);
        category.setCreatedBy("SYSTEM");
        category.setUpdatedBy("SYSTEM");

        // Set parent category if provided
        if (request.getParentCategoryId() != null) {
            ExpenseCategory parentCategory = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentCategoryId()));
            category.setParentCategory(parentCategory);
        }

        ExpenseCategory savedCategory = categoryRepository.save(category);

        log.info("Expense category created successfully with code: {}", savedCategory.getCategoryCode());

        return categoryMapper.toResponse(savedCategory);
    }

    private String generateCategoryCode() {
        String prefix = "EXPCAT";
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String categoryCode = prefix + "-" + randomPart;

        while (categoryRepository.existsByCategoryCode(categoryCode)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            categoryCode = prefix + "-" + randomPart;
        }

        return categoryCode;
    }

    @Override
    public ExpenseCategoryResponse updateCategory(Long id, ExpenseCategoryRequest request) {
        log.info("Updating expense category with ID: {}", id);

        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + id));

        // Check name uniqueness if changed
        if (!category.getName().equals(request.getName()) &&
                categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Expense category with name " + request.getName() + " already exists");
        }

        // Update fields
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setType(request.getType());
        category.setBudgetAmount(request.getBudgetAmount());
        category.setIsActive(request.getIsActive());
        category.setNotes(request.getNotes());
        category.setUpdatedBy("SYSTEM");

        // Update parent category if changed
        if (request.getParentCategoryId() != null) {
            if (request.getParentCategoryId().equals(id)) {
                throw new BusinessException("Category cannot be its own parent");
            }
            ExpenseCategory parentCategory = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentCategoryId()));
            category.setParentCategory(parentCategory);
        } else {
            category.setParentCategory(null);
        }

        ExpenseCategory updatedCategory = categoryRepository.save(category);
        log.info("Expense category updated successfully with ID: {}", updatedCategory.getId());

        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    public ExpenseCategoryResponse getCategoryById(Long id) {
        log.debug("Fetching expense category with ID: {}", id);

        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + id));

        ExpenseCategoryResponse response = categoryMapper.toResponse(category);
        response.setTotalExpenses(calculateTotalExpensesForCategory(id));
        response.setRemainingBudget(calculateRemainingBudget(category));

        return response;
    }

    private BigDecimal calculateTotalExpensesForCategory(Long categoryId) {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1).atTime(23, 59, 59);

        return expenseRepository.getTotalExpensesByCategoryForPeriod(categoryId, startOfMonth, endOfMonth);
    }

    private BigDecimal calculateRemainingBudget(ExpenseCategory category) {
        if (category.getBudgetAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal spent = calculateTotalExpensesForCategory(category.getId());
        return category.getBudgetAmount().subtract(spent);
    }

    @Override
    public ExpenseCategoryResponse getCategoryByCode(String categoryCode) {
        log.debug("Fetching expense category with code: {}", categoryCode);

        ExpenseCategory category = categoryRepository.findByCategoryCode(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with code: " + categoryCode));

        return categoryMapper.toResponse(category);
    }

    @Override
    public ExpenseCategoryResponse getCategoryByName(String name) {
        log.debug("Fetching expense category with name: {}", name);

        ExpenseCategory category = categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with name: " + name));

        return categoryMapper.toResponse(category);
    }

    @Override
    public Page<ExpenseCategoryResponse> getAllCategories(Pageable pageable) {
        log.debug("Fetching all expense categories with pagination");

        return categoryRepository.findAll(pageable)
                .map(category -> {
                    ExpenseCategoryResponse response = categoryMapper.toResponse(category);
                    response.setTotalExpenses(calculateTotalExpensesForCategory(category.getId()));
                    response.setRemainingBudget(calculateRemainingBudget(category));
                    return response;
                });
    }

    @Override
    public List<ExpenseCategoryResponse> getRootCategories() {
        log.debug("Fetching root expense categories");

        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpenseCategoryResponse> getSubCategories(Long parentId) {
        log.debug("Fetching sub-categories for parent ID: {}", parentId);

        return categoryRepository.findByParentCategoryId(parentId).stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpenseCategoryResponse> getCategoriesByType(String type) {
        log.debug("Fetching expense categories by type: {}", type);

        return categoryRepository.findByType(type).stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpenseCategoryResponse> searchCategories(String searchTerm) {
        log.debug("Searching expense categories with term: {}", searchTerm);

        return categoryRepository.searchCategories(searchTerm).stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCategory(Long id) {
        log.info("Deleting expense category with ID: {}", id);

        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + id));

        // Check if category has expenses
        if (category.getExpenses() != null && !category.getExpenses().isEmpty()) {
            throw new BusinessException("Cannot delete category with existing expenses");
        }

        // Check if category has sub-categories
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            throw new BusinessException("Cannot delete category with sub-categories");
        }

        categoryRepository.delete(category);
        log.info("Expense category deleted successfully with ID: {}", id);
    }

    @Override
    public void activateCategory(Long id) {
        log.info("Activating expense category with ID: {}", id);

        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + id));

        category.setIsActive(true);
        category.setUpdatedBy("SYSTEM");
        categoryRepository.save(category);
    }

    @Override
    public void deactivateCategory(Long id) {
        log.info("Deactivating expense category with ID: {}", id);

        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + id));

        category.setIsActive(false);
        category.setUpdatedBy("SYSTEM");
        categoryRepository.save(category);
    }

    @Override
    public ExpenseCategoryResponse updateBudget(Long id, BigDecimal budgetAmount) {
        log.info("Updating budget for expense category ID: {} to {}", id, budgetAmount);

        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + id));

        category.setBudgetAmount(budgetAmount);
        category.setUpdatedBy("SYSTEM");

        ExpenseCategory updatedCategory = categoryRepository.save(category);

        ExpenseCategoryResponse response = categoryMapper.toResponse(updatedCategory);
        response.setTotalExpenses(calculateTotalExpensesForCategory(id));
        response.setRemainingBudget(calculateRemainingBudget(updatedCategory));

        return response;
    }

    @Override
    public BigDecimal getTotalExpensesForCategory(Long categoryId, String period) {
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();

        switch (period.toUpperCase()) {
            case "TODAY":
                startDate = LocalDate.now().atStartOfDay();
                break;
            case "WEEK":
                startDate = LocalDate.now().minusDays(7).atStartOfDay();
                break;
            case "MONTH":
                startDate = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                break;
            case "QUARTER":
                startDate = LocalDate.now().minusMonths(3).withDayOfMonth(1).atStartOfDay();
                break;
            case "YEAR":
                startDate = LocalDate.now().withDayOfYear(1).atStartOfDay();
                break;
            default:
                startDate = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        }

        return expenseRepository.getTotalExpensesByCategoryForPeriod(categoryId, startDate, endDate);
    }

    @Override
    public boolean isCategoryNameUnique(String name) {
        return !categoryRepository.existsByName(name);
    }

    @Override
    public boolean isCategoryCodeUnique(String categoryCode) {
        return !categoryRepository.existsByCategoryCode(categoryCode);
    }
}