package com.retailmanagement.modules.expense.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.expense.dto.request.ExpenseApprovalRequest;
import com.retailmanagement.modules.expense.dto.request.ExpenseAttachmentRequest;
import com.retailmanagement.modules.expense.dto.request.ExpenseRequest;
import com.retailmanagement.modules.expense.dto.response.ExpenseResponse;
import com.retailmanagement.modules.expense.dto.response.ExpenseSummaryResponse;
import com.retailmanagement.modules.expense.enums.ExpenseStatus;
import com.retailmanagement.modules.expense.enums.PaymentMethod;
import com.retailmanagement.modules.expense.mapper.ExpenseAttachmentMapper;
import com.retailmanagement.modules.expense.mapper.ExpenseMapper;
import com.retailmanagement.modules.expense.model.Expense;
import com.retailmanagement.modules.expense.model.ExpenseAttachment;
import com.retailmanagement.modules.expense.model.ExpenseCategory;
import com.retailmanagement.modules.expense.repository.ExpenseCategoryRepository;
import com.retailmanagement.modules.expense.repository.ExpenseRepository;
import com.retailmanagement.modules.expense.service.ExpenseService;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;
    private final ExpenseAttachmentMapper attachmentMapper;

    @Override
    public ExpenseResponse createExpense(ExpenseRequest request) {
        log.info("Creating new expense for category ID: {}", request.getCategoryId());

        ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + request.getCategoryId()));

        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
        }

        // Check budget if category has budget
        if (category.getBudgetAmount() != null) {
            BigDecimal spentThisMonth = expenseRepository.getTotalExpensesByCategoryForPeriod(
                    category.getId(),
                    LocalDate.now().withDayOfMonth(1).atStartOfDay(),
                    LocalDateTime.now()
            );

            BigDecimal newTotal = spentThisMonth.add(request.getAmount());
            if (newTotal.compareTo(category.getBudgetAmount()) > 0) {
                log.warn("Expense will exceed monthly budget for category: {}", category.getName());
                // You might want to throw an exception or just warn based on business rules
            }
        }

        Expense expense = expenseMapper.toEntity(request);

        // Generate expense number
        expense.setExpenseNumber(generateExpenseNumber());
        expense.setCategory(category);
        expense.setUser(user);
        expense.setStatus(ExpenseStatus.PENDING_APPROVAL);
        expense.setCreatedBy(user != null ? user.getUsername() : "SYSTEM");
        expense.setUpdatedBy(user != null ? user.getUsername() : "SYSTEM");

        // Save attachments if any
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            saveExpenseAttachments(expense, request.getAttachments());
        }

        Expense savedExpense = expenseRepository.save(expense);

        // Update category allocated amount
        category.setAllocatedAmount(category.getAllocatedAmount().add(request.getAmount()));
        categoryRepository.save(category);

        log.info("Expense created successfully with number: {}", savedExpense.getExpenseNumber());

        return expenseMapper.toResponse(savedExpense);
    }

    private String generateExpenseNumber() {
        String yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String expenseNumber = "EXP-" + yearMonth + "-" + randomPart;

        while (expenseRepository.existsByExpenseNumber(expenseNumber)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            expenseNumber = "EXP-" + yearMonth + "-" + randomPart;
        }

        return expenseNumber;
    }

    private void saveExpenseAttachments(Expense expense, List<ExpenseAttachmentRequest> attachmentRequests) {
        for (ExpenseAttachmentRequest attachmentRequest : attachmentRequests) {
            ExpenseAttachment attachment = attachmentMapper.toEntity(attachmentRequest);
            attachment.setExpense(expense);
            attachment.setUploadedBy(expense.getCreatedBy());
            attachment.setUploadedAt(LocalDateTime.now());
            expense.getAttachments().add(attachment);
        }
    }

    @Override
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        log.info("Updating expense with ID: {}", id);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        // Check if expense can be updated
        if (expense.getStatus() != ExpenseStatus.DRAFT &&
                expense.getStatus() != ExpenseStatus.PENDING_APPROVAL) {
            throw new BusinessException("Cannot update expense in " + expense.getStatus() + " status");
        }

        // Update category if changed
        if (!expense.getCategory().getId().equals(request.getCategoryId())) {
            ExpenseCategory newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + request.getCategoryId()));

            // Adjust allocated amounts
            expense.getCategory().setAllocatedAmount(
                    expense.getCategory().getAllocatedAmount().subtract(expense.getAmount())
            );
            categoryRepository.save(expense.getCategory());

            newCategory.setAllocatedAmount(newCategory.getAllocatedAmount().add(request.getAmount()));
            categoryRepository.save(newCategory);

            expense.setCategory(newCategory);
        } else {
            // Adjust allocated amount if amount changed
            if (!expense.getAmount().equals(request.getAmount())) {
                BigDecimal difference = request.getAmount().subtract(expense.getAmount());
                expense.getCategory().setAllocatedAmount(
                        expense.getCategory().getAllocatedAmount().add(difference)
                );
                categoryRepository.save(expense.getCategory());
            }
        }

        // Update fields
        expense.setExpenseDate(request.getExpenseDate());
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setVendor(request.getVendor());
        expense.setVendorInvoiceNumber(request.getVendorInvoiceNumber());
        expense.setReferenceNumber(request.getReferenceNumber());
        expense.setPaidTo(request.getPaidTo());
        expense.setNotes(request.getNotes());
        expense.setIsReimbursable(request.getIsReimbursable());
        expense.setIsBillable(request.getIsBillable());
        expense.setCustomerId(request.getCustomerId());
        expense.setProjectId(request.getProjectId());
        expense.setUpdatedBy(expense.getCreatedBy());

        Expense updatedExpense = expenseRepository.save(expense);
        log.info("Expense updated successfully with ID: {}", updatedExpense.getId());

        return expenseMapper.toResponse(updatedExpense);
    }

    @Override
    public ExpenseResponse getExpenseById(Long id) {
        log.debug("Fetching expense with ID: {}", id);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        return expenseMapper.toResponse(expense);
    }

    @Override
    public ExpenseResponse getExpenseByNumber(String expenseNumber) {
        log.debug("Fetching expense with number: {}", expenseNumber);

        Expense expense = expenseRepository.findByExpenseNumber(expenseNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with number: " + expenseNumber));

        return expenseMapper.toResponse(expense);
    }

    @Override
    public Page<ExpenseResponse> getAllExpenses(Pageable pageable) {
        log.debug("Fetching all expenses with pagination");

        return expenseRepository.findAll(pageable)
                .map(expenseMapper::toResponse);
    }

    @Override
    public List<ExpenseResponse> getExpensesByCategory(Long categoryId) {
        log.debug("Fetching expenses for category ID: {}", categoryId);

        return expenseRepository.findByCategoryId(categoryId).stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ExpenseResponse> getExpensesByCategory(Long categoryId, Pageable pageable) {
        log.debug("Fetching expenses for category ID: {} with pagination", categoryId);

        return expenseRepository.findByCategoryId(categoryId, pageable)
                .map(expenseMapper::toResponse);
    }

    @Override
    public List<ExpenseResponse> getExpensesByUser(Long userId) {
        log.debug("Fetching expenses for user ID: {}", userId);

        return expenseRepository.findByUserId(userId).stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ExpenseResponse> getExpensesByUser(Long userId, Pageable pageable) {
        log.debug("Fetching expenses for user ID: {} with pagination", userId);

        return expenseRepository.findByUserId(userId, pageable)
                .map(expenseMapper::toResponse);
    }

    @Override
    public List<ExpenseResponse> getExpensesByStatus(ExpenseStatus status) {
        log.debug("Fetching expenses with status: {}", status);

        return expenseRepository.findByStatus(status).stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ExpenseResponse> getExpensesByStatus(ExpenseStatus status, Pageable pageable) {
        log.debug("Fetching expenses with status: {} with pagination", status);

        return expenseRepository.findByStatus(status, pageable)
                .map(expenseMapper::toResponse);
    }

    @Override
    public List<ExpenseResponse> getExpensesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching expenses between {} and {}", startDate, endDate);

        return expenseRepository.findByExpenseDateBetween(startDate, endDate).stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpenseResponse> getExpensesByVendor(String vendor) {
        log.debug("Fetching expenses for vendor: {}", vendor);

        return expenseRepository.findByVendor(vendor).stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ExpenseResponse approveExpense(Long id, ExpenseApprovalRequest request) {
        log.info("Approving expense with ID: {}", id);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (expense.getStatus() != ExpenseStatus.PENDING_APPROVAL) {
            throw new BusinessException("Expense is not pending approval");
        }

        if (request.getApproved()) {
            expense.setStatus(ExpenseStatus.APPROVED);
            expense.setApprovedAt(LocalDateTime.now());
            expense.setApprovedBy("SYSTEM"); // In real app, get from SecurityContext

            // If payment method is cash, automatically mark as paid
            if (expense.getPaymentMethod() == PaymentMethod.CASH) {
                expense.setStatus(ExpenseStatus.PAID);
            }
        } else {
            return rejectExpense(id, request.getRejectionReason());
        }

        expense.setNotes(expense.getNotes() + " [Approval comments: " + request.getComments() + "]");
        expense.setUpdatedBy(expense.getCreatedBy());

        Expense approvedExpense = expenseRepository.save(expense);
        log.info("Expense approved successfully with ID: {}", id);

        return expenseMapper.toResponse(approvedExpense);
    }

    @Override
    public ExpenseResponse rejectExpense(Long id, String reason) {
        log.info("Rejecting expense with ID: {}", id);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (expense.getStatus() != ExpenseStatus.PENDING_APPROVAL) {
            throw new BusinessException("Expense is not pending approval");
        }

        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setRejectionReason(reason);
        expense.setUpdatedBy(expense.getCreatedBy());

        // Reverse allocated amount
        expense.getCategory().setAllocatedAmount(
                expense.getCategory().getAllocatedAmount().subtract(expense.getAmount())
        );
        categoryRepository.save(expense.getCategory());

        Expense rejectedExpense = expenseRepository.save(expense);
        log.info("Expense rejected successfully with ID: {}", id);

        return expenseMapper.toResponse(rejectedExpense);
    }

    @Override
    public ExpenseResponse markAsPaid(Long id) {
        log.info("Marking expense as paid with ID: {}", id);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (expense.getStatus() != ExpenseStatus.APPROVED) {
            throw new BusinessException("Only approved expenses can be marked as paid");
        }

        expense.setStatus(ExpenseStatus.PAID);
        expense.setUpdatedBy(expense.getCreatedBy());

        Expense paidExpense = expenseRepository.save(expense);
        log.info("Expense marked as paid successfully with ID: {}", id);

        return expenseMapper.toResponse(paidExpense);
    }

    @Override
    public void cancelExpense(Long id, String reason) {
        log.info("Cancelling expense with ID: {}", id);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (expense.getStatus() == ExpenseStatus.PAID ||
                expense.getStatus() == ExpenseStatus.CANCELLED) {
            throw new BusinessException("Cannot cancel paid or already cancelled expense");
        }

        expense.setStatus(ExpenseStatus.CANCELLED);
        expense.setNotes(expense.getNotes() + " [CANCELLED: " + reason + "]");
        expense.setUpdatedBy(expense.getCreatedBy());

        // Reverse allocated amount
        expense.getCategory().setAllocatedAmount(
                expense.getCategory().getAllocatedAmount().subtract(expense.getAmount())
        );
        categoryRepository.save(expense.getCategory());

        expenseRepository.save(expense);
        log.info("Expense cancelled successfully with ID: {}", id);
    }

    @Override
    public ExpenseResponse uploadReceipt(Long id, String receiptUrl) {
        log.info("Uploading receipt for expense ID: {}", id);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        expense.setReceiptUrl(receiptUrl);
        expense.setUpdatedBy(expense.getCreatedBy());

        Expense updatedExpense = expenseRepository.save(expense);
        log.info("Receipt uploaded successfully for expense ID: {}", id);

        return expenseMapper.toResponse(updatedExpense);
    }

    @Override
    public BigDecimal getTotalExpenses(LocalDateTime startDate, LocalDateTime endDate) {
        return expenseRepository.getTotalExpensesForPeriod(startDate, endDate);
    }

    @Override
    public ExpenseSummaryResponse getExpenseSummary(String period) {
        log.debug("Generating expense summary for period: {}", period);

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

        BigDecimal totalExpenses = expenseRepository.getTotalExpensesForPeriod(startDate, endDate);
        BigDecimal approvedExpenses = expenseRepository.getTotalExpensesByStatusForPeriod(
                ExpenseStatus.APPROVED, startDate, endDate);
        BigDecimal pendingExpenses = expenseRepository.getTotalExpensesByStatusForPeriod(
                ExpenseStatus.PENDING_APPROVAL, startDate, endDate);
        BigDecimal paidExpenses = expenseRepository.getTotalExpensesByStatusForPeriod(
                ExpenseStatus.PAID, startDate, endDate);

        // Get expenses by category
        List<Object[]> categoryData = expenseRepository.getExpensesGroupedByCategory(startDate, endDate);
        Map<String, BigDecimal> expensesByCategory = new HashMap<>();
        List<ExpenseSummaryResponse.CategoryBreakdown> topCategories = new ArrayList<>();

        for (Object[] row : categoryData) {
            String categoryName = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            expensesByCategory.put(categoryName, amount);

            double percentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0 ?
                    amount.divide(totalExpenses, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).doubleValue() : 0;

            topCategories.add(ExpenseSummaryResponse.CategoryBreakdown.builder()
                    .categoryName(categoryName)
                    .amount(amount)
                    .percentage(percentage)
                    .build());
        }

        // Get expenses by month
        Map<String, BigDecimal> expensesByMonth = new LinkedHashMap<>();
        LocalDateTime current = startDate;
        while (current.isBefore(endDate)) {
            LocalDateTime monthStart = current.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

            BigDecimal monthTotal = expenseRepository.getTotalExpensesForPeriod(monthStart, monthEnd);
            String monthKey = monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            expensesByMonth.put(monthKey, monthTotal);

            current = current.plusMonths(1);
        }

        // Get top vendors
        Pageable topVendorsPageable = PageRequest.of(0, 5);
        List<Object[]> vendorData = expenseRepository.getTopVendors(startDate, endDate, topVendorsPageable);
        List<ExpenseSummaryResponse.VendorBreakdown> topVendors = new ArrayList<>();

        for (Object[] row : vendorData) {
            topVendors.add(ExpenseSummaryResponse.VendorBreakdown.builder()
                    .vendor((String) row[0])
                    .amount((BigDecimal) row[1])
                    .count(((Long) row[2]).intValue())
                    .build());
        }

        // Sort top categories by amount descending
        topCategories.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        return ExpenseSummaryResponse.builder()
                .totalExpenses(totalExpenses)
                .approvedExpenses(approvedExpenses)
                .pendingExpenses(pendingExpenses)
                .paidExpenses(paidExpenses)
                .expensesByCategory(expensesByCategory)
                .expensesByMonth(expensesByMonth)
                .topCategories(topCategories.stream().limit(5).collect(Collectors.toList()))
                .topVendors(topVendors)
                .build();
    }

    @Override
    public Long getPendingApprovalCount() {
        return expenseRepository.findByStatus(ExpenseStatus.PENDING_APPROVAL).stream().count();
    }

    @Override
    public List<ExpenseResponse> getRecentExpenses(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("expenseDate").descending());
        return expenseRepository.findAll(pageable)
                .map(expenseMapper::toResponse)
                .getContent();
    }

    @Override
    public boolean isExpenseNumberUnique(String expenseNumber) {
        return !expenseRepository.existsByExpenseNumber(expenseNumber);
    }
}