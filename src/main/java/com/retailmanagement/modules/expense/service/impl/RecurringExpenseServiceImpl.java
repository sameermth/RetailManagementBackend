package com.retailmanagement.modules.expense.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.expense.dto.request.ExpenseRequest;
import com.retailmanagement.modules.expense.dto.request.RecurringExpenseRequest;
import com.retailmanagement.modules.expense.dto.response.RecurringExpenseResponse;
import com.retailmanagement.modules.expense.enums.ExpenseStatus;
import com.retailmanagement.modules.expense.enums.PaymentMethod;
import com.retailmanagement.modules.expense.enums.RecurringFrequency;
import com.retailmanagement.modules.expense.mapper.RecurringExpenseMapper;
import com.retailmanagement.modules.expense.model.ExpenseCategory;
import com.retailmanagement.modules.expense.model.RecurringExpense;
import com.retailmanagement.modules.expense.repository.ExpenseCategoryRepository;
import com.retailmanagement.modules.expense.repository.RecurringExpenseRepository;
import com.retailmanagement.modules.expense.service.ExpenseService;
import com.retailmanagement.modules.expense.service.RecurringExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecurringExpenseServiceImpl implements RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseService expenseService;
    private final RecurringExpenseMapper recurringExpenseMapper;

    @Override
    public RecurringExpenseResponse createRecurringExpense(RecurringExpenseRequest request) {
        log.info("Creating new recurring expense for category ID: {}", request.getCategoryId());

        ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + request.getCategoryId()));

        RecurringExpense recurringExpense = recurringExpenseMapper.toEntity(request);

        // Generate recurring expense number
        recurringExpense.setRecurringExpenseNumber(generateRecurringExpenseNumber());
        recurringExpense.setCategory(category);
        recurringExpense.setStatus(ExpenseStatus.APPROVED);
        recurringExpense.setOccurrencesGenerated(0);
        recurringExpense.setNextGenerationDate(calculateNextGenerationDate(request.getStartDate(), request.getFrequency()));
        recurringExpense.setCreatedBy("SYSTEM");
        recurringExpense.setUpdatedBy("SYSTEM");

        RecurringExpense savedRecurringExpense = recurringExpenseRepository.save(recurringExpense);

        log.info("Recurring expense created successfully with number: {}", savedRecurringExpense.getRecurringExpenseNumber());

        return recurringExpenseMapper.toResponse(savedRecurringExpense);
    }

    private String generateRecurringExpenseNumber() {
        String prefix = "RECEXP";
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String recurringExpenseNumber = prefix + "-" + randomPart;

        while (recurringExpenseRepository.existsByRecurringExpenseNumber(recurringExpenseNumber)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            recurringExpenseNumber = prefix + "-" + randomPart;
        }

        return recurringExpenseNumber;
    }

    private LocalDate calculateNextGenerationDate(LocalDate startDate, RecurringFrequency frequency) {
        LocalDate today = LocalDate.now();

        if (startDate.isAfter(today)) {
            return startDate;
        }

        switch (frequency) {
            case DAILY:
                return today.plusDays(1);
            case WEEKLY:
                return today.plusWeeks(1);
            case BI_WEEKLY:
                return today.plusWeeks(2);
            case MONTHLY:
                return today.plusMonths(1);
            case QUARTERLY:
                return today.plusMonths(3);
            case HALF_YEARLY:
                return today.plusMonths(6);
            case YEARLY:
                return today.plusYears(1);
            default:
                return today.plusMonths(1);
        }
    }

    @Override
    public RecurringExpenseResponse updateRecurringExpense(Long id, RecurringExpenseRequest request) {
        log.info("Updating recurring expense with ID: {}", id);

        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with id: " + id));

        // Update category if changed
        if (!recurringExpense.getCategory().getId().equals(request.getCategoryId())) {
            ExpenseCategory newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + request.getCategoryId()));
            recurringExpense.setCategory(newCategory);
        }

        // Update fields
        recurringExpense.setDescription(request.getDescription());
        recurringExpense.setAmount(request.getAmount());
        recurringExpense.setFrequency(request.getFrequency());
        recurringExpense.setStartDate(request.getStartDate());
        recurringExpense.setEndDate(request.getEndDate());
        recurringExpense.setOccurrenceCount(request.getOccurrenceCount());
        recurringExpense.setVendor(request.getVendor());
        recurringExpense.setPaymentMethod(request.getPaymentMethod());
        recurringExpense.setNotes(request.getNotes());
        recurringExpense.setIsActive(request.getIsActive());
        recurringExpense.setUpdatedBy("SYSTEM");

        // Recalculate next generation date if needed
        if (recurringExpense.getNextGenerationDate() == null ||
                !recurringExpense.getStartDate().equals(request.getStartDate()) ||
                recurringExpense.getFrequency() != request.getFrequency()) {
            recurringExpense.setNextGenerationDate(
                    calculateNextGenerationDate(request.getStartDate(), request.getFrequency())
            );
        }

        RecurringExpense updatedRecurringExpense = recurringExpenseRepository.save(recurringExpense);
        log.info("Recurring expense updated successfully with ID: {}", updatedRecurringExpense.getId());

        return recurringExpenseMapper.toResponse(updatedRecurringExpense);
    }

    @Override
    public RecurringExpenseResponse getRecurringExpenseById(Long id) {
        log.debug("Fetching recurring expense with ID: {}", id);

        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with id: " + id));

        return recurringExpenseMapper.toResponse(recurringExpense);
    }

    @Override
    public RecurringExpenseResponse getRecurringExpenseByNumber(String recurringExpenseNumber) {
        log.debug("Fetching recurring expense with number: {}", recurringExpenseNumber);

        RecurringExpense recurringExpense = recurringExpenseRepository.findByRecurringExpenseNumber(recurringExpenseNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with number: " + recurringExpenseNumber));

        return recurringExpenseMapper.toResponse(recurringExpense);
    }

    @Override
    public Page<RecurringExpenseResponse> getAllRecurringExpenses(Pageable pageable) {
        log.debug("Fetching all recurring expenses with pagination");

        return recurringExpenseRepository.findAll(pageable)
                .map(recurringExpenseMapper::toResponse);
    }

    @Override
    public List<RecurringExpenseResponse> getRecurringExpensesByCategory(Long categoryId) {
        log.debug("Fetching recurring expenses for category ID: {}", categoryId);

        return recurringExpenseRepository.findByCategoryId(categoryId).stream()
                .map(recurringExpenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecurringExpenseResponse> getRecurringExpensesByFrequency(RecurringFrequency frequency) {
        log.debug("Fetching recurring expenses with frequency: {}", frequency);

        return recurringExpenseRepository.findByFrequency(frequency).stream()
                .map(recurringExpenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteRecurringExpense(Long id) {
        log.info("Deleting recurring expense with ID: {}", id);

        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with id: " + id));

        recurringExpenseRepository.delete(recurringExpense);
        log.info("Recurring expense deleted successfully with ID: {}", id);
    }

    @Override
    public void activateRecurringExpense(Long id) {
        log.info("Activating recurring expense with ID: {}", id);

        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with id: " + id));

        recurringExpense.setIsActive(true);
        recurringExpense.setUpdatedBy("SYSTEM");
        recurringExpenseRepository.save(recurringExpense);
    }

    @Override
    public void deactivateRecurringExpense(Long id) {
        log.info("Deactivating recurring expense with ID: {}", id);

        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with id: " + id));

        recurringExpense.setIsActive(false);
        recurringExpense.setUpdatedBy("SYSTEM");
        recurringExpenseRepository.save(recurringExpense);
    }

    @Override
    @Scheduled(cron = "0 0 1 * * *") // Run at 1 AM every day
    public void generateRecurringExpenses() {
        log.info("Starting recurring expenses generation at {}", LocalDateTime.now());

        List<RecurringExpenseResponse> dueExpenses = getRecurringExpensesDueForGeneration();

        for (RecurringExpenseResponse recurringExpense : dueExpenses) {
            try {
                int generated = generateExpensesForRecurring(recurringExpense.getId());
                log.info("Generated {} expenses for recurring expense ID: {}", generated, recurringExpense.getId());
            } catch (Exception e) {
                log.error("Failed to generate expenses for recurring expense ID: {}", recurringExpense.getId(), e);
            }
        }

        log.info("Completed recurring expenses generation. Processed {} items", dueExpenses.size());
    }

    @Override
    public int generateExpensesForRecurring(Long id) {
        log.info("Generating expenses for recurring expense ID: {}", id);

        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with id: " + id));

        if (!recurringExpense.getIsActive()) {
            throw new BusinessException("Cannot generate expenses for inactive recurring expense");
        }

        if (recurringExpense.getEndDate() != null &&
                recurringExpense.getEndDate().isBefore(LocalDate.now())) {
            recurringExpense.setIsActive(false);
            recurringExpense.setStatus(ExpenseStatus.COMPLETED);
            recurringExpenseRepository.save(recurringExpense);
            return 0;
        }

        if (recurringExpense.getOccurrenceCount() != null &&
                recurringExpense.getOccurrencesGenerated() >= recurringExpense.getOccurrenceCount()) {
            recurringExpense.setIsActive(false);
            recurringExpense.setStatus(ExpenseStatus.COMPLETED);
            recurringExpenseRepository.save(recurringExpense);
            return 0;
        }

        // Create expense from recurring template
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setCategoryId(recurringExpense.getCategory().getId());
        expenseRequest.setExpenseDate(LocalDateTime.now());
        expenseRequest.setDescription(recurringExpense.getDescription());
        expenseRequest.setAmount(recurringExpense.getAmount());
        expenseRequest.setPaymentMethod(
                recurringExpense.getPaymentMethod() != null ?
                        PaymentMethod.valueOf(recurringExpense.getPaymentMethod()) : null
        );
        expenseRequest.setVendor(recurringExpense.getVendor());
        expenseRequest.setNotes("Generated from recurring expense: " + recurringExpense.getRecurringExpenseNumber());

        expenseService.createExpense(expenseRequest);

        // Update recurring expense
        recurringExpense.setOccurrencesGenerated(recurringExpense.getOccurrencesGenerated() + 1);
        recurringExpense.setNextGenerationDate(calculateNextGenerationDate(
                recurringExpense.getNextGenerationDate(),
                recurringExpense.getFrequency()
        ));
        recurringExpense.setUpdatedBy("SYSTEM");

        recurringExpenseRepository.save(recurringExpense);

        return 1;
    }

    private LocalDate calculateNextGenerationDate1(LocalDate currentDate, RecurringFrequency frequency) {
        switch (frequency) {
            case DAILY:
                return currentDate.plusDays(1);
            case WEEKLY:
                return currentDate.plusWeeks(1);
            case BI_WEEKLY:
                return currentDate.plusWeeks(2);
            case MONTHLY:
                return currentDate.plusMonths(1);
            case QUARTERLY:
                return currentDate.plusMonths(3);
            case HALF_YEARLY:
                return currentDate.plusMonths(6);
            case YEARLY:
                return currentDate.plusYears(1);
            default:
                return currentDate.plusMonths(1);
        }
    }

    @Override
    public List<RecurringExpenseResponse> getRecurringExpensesDueForGeneration() {
        log.debug("Fetching recurring expenses due for generation");

        LocalDate today = LocalDate.now();
        return recurringExpenseRepository.findRecurringExpensesDueForGeneration(today).stream()
                .map(recurringExpenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isRecurringExpenseNumberUnique(String recurringExpenseNumber) {
        return !recurringExpenseRepository.existsByRecurringExpenseNumber(recurringExpenseNumber);
    }
}