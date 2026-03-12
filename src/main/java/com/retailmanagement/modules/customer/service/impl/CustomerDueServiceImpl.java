package com.retailmanagement.modules.customer.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.customer.dto.request.CustomerDueRequest;
import com.retailmanagement.modules.customer.dto.response.CustomerDueResponse;
import com.retailmanagement.modules.customer.enums.DueStatus;
import com.retailmanagement.modules.customer.mapper.CustomerDueMapper;
import com.retailmanagement.modules.customer.model.Customer;
import com.retailmanagement.modules.customer.model.CustomerDue;
import com.retailmanagement.modules.customer.repository.CustomerDueRepository;
import com.retailmanagement.modules.customer.repository.CustomerRepository;
import com.retailmanagement.modules.customer.service.CustomerDueService;
import com.retailmanagement.modules.customer.service.CustomerService;
import com.retailmanagement.modules.notification.service.NotificationService;
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
public class CustomerDueServiceImpl implements CustomerDueService {

    private final CustomerDueRepository dueRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final NotificationService notificationService;
    private final CustomerDueMapper dueMapper;

    @Override
    public CustomerDueResponse createDue(CustomerDueRequest request) {
        log.info("Creating due for customer ID: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        CustomerDue due = dueMapper.toEntity(request);
        due.setCustomer(customer);
        due.setDueReference(generateDueReference());
        due.setOriginalAmount(request.getAmount());
        due.setRemainingAmount(request.getAmount());
        due.setPaidAmount(BigDecimal.ZERO);
        due.setStatus(DueStatus.PENDING);
        due.setReminderCount(0);

        CustomerDue savedDue = dueRepository.save(due);

        // Update customer's total due amount
        customerService.updateCustomerDue(customer.getId(), request.getAmount());

        log.info("Due created successfully with reference: {}", savedDue.getDueReference());

        return dueMapper.toResponse(savedDue);
    }

    @Override
    public CustomerDueResponse createDueFromSale(Long saleId) {
        log.info("Creating due from sale ID: {}", saleId);

        // This method would be called from Sales module when a credit sale is made
        // Implementation would fetch sale details and create a due entry
        // For now, we'll create a placeholder implementation
        throw new UnsupportedOperationException("Method to be implemented with Sales module integration");
    }

    private String generateDueReference() {
        String year = String.valueOf(LocalDate.now().getYear());
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "DUE-" + year + "-" + randomPart;
    }

    @Override
    public CustomerDueResponse getDueById(Long id) {
        log.debug("Fetching due with ID: {}", id);

        CustomerDue due = dueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Due not found with id: " + id));

        return dueMapper.toResponse(due);
    }

    @Override
    public CustomerDueResponse getDueByReference(String dueReference) {
        log.debug("Fetching due with reference: {}", dueReference);

        CustomerDue due = dueRepository.findByDueReference(dueReference)
                .orElseThrow(() -> new ResourceNotFoundException("Due not found with reference: " + dueReference));

        return dueMapper.toResponse(due);
    }

    @Override
    public Page<CustomerDueResponse> getDuesByCustomer(Long customerId, Pageable pageable) {
        log.debug("Fetching dues for customer ID: {} with pagination", customerId);

        return dueRepository.findByCustomerId(customerId, pageable)
                .map(dueMapper::toResponse);
    }

    @Override
    public List<CustomerDueResponse> getOverdueDues() {
        log.debug("Fetching overdue dues");

        LocalDate today = LocalDate.now();
        return dueRepository.findOverdueDues(today).stream()
                .map(dueMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerDueResponse> getDuesDueBetween(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching dues due between {} and {}", startDate, endDate);

        return dueRepository.findDuesInDateRange(startDate, endDate).stream()
                .map(dueMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerDueResponse recordPayment(Long dueId, BigDecimal paymentAmount, String paymentReference) {
        log.info("Recording payment of {} for due ID: {}", paymentAmount, dueId);

        CustomerDue due = dueRepository.findById(dueId)
                .orElseThrow(() -> new ResourceNotFoundException("Due not found with id: " + dueId));

        if (due.getStatus() == DueStatus.PAID) {
            throw new BusinessException("Due is already fully paid");
        }

        if (paymentAmount.compareTo(due.getRemainingAmount()) > 0) {
            throw new BusinessException("Payment amount exceeds remaining due amount");
        }

        // Update due
        BigDecimal newPaidAmount = due.getPaidAmount().add(paymentAmount);
        BigDecimal newRemainingAmount = due.getRemainingAmount().subtract(paymentAmount);

        due.setPaidAmount(newPaidAmount);
        due.setRemainingAmount(newRemainingAmount);
        due.setLastPaymentDate(LocalDateTime.now());

        // Update status
        if (newRemainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            due.setStatus(DueStatus.PAID);
        } else {
            due.setStatus(DueStatus.PARTIALLY_PAID);
        }

        // Update customer's total due amount (reduce by payment amount)
        customerService.updateCustomerDue(due.getCustomer().getId(), paymentAmount.negate());

        CustomerDue updatedDue = dueRepository.save(due);

        // Send payment confirmation notification
        sendPaymentConfirmation(updatedDue, paymentAmount, paymentReference);

        log.info("Payment recorded successfully for due ID: {}", dueId);

        return dueMapper.toResponse(updatedDue);
    }

    private void sendPaymentConfirmation(CustomerDue due, BigDecimal paymentAmount, String paymentReference) {
        Customer customer = due.getCustomer();
        String message = String.format(
                "Dear %s, we have received your payment of ₹%.2f towards invoice %s. " +
                        "Remaining balance: ₹%.2f. Thank you for your business!",
                customer.getName(),
                paymentAmount,
                due.getInvoiceNumber() != null ? due.getInvoiceNumber() : due.getDueReference(),
                due.getRemainingAmount()
        );

        if (customer.getEmail() != null) {
            notificationService.sendEmailNotification(
                    customer.getEmail(),
                    "Payment Received - Thank You",
                    message
            );
        }

        if (customer.getPhone() != null) {
            notificationService.sendSmsNotification(
                    customer.getPhone(),
                    message
            );
        }

        // Create in-app notification
        notificationService.sendInAppNotification(
                customer.getId(),
                message
        );
    }

    @Override
    public CustomerDueResponse updateDueStatus(Long dueId, String status) {
        log.info("Updating status for due ID: {} to {}", dueId, status);

        CustomerDue due = dueRepository.findById(dueId)
                .orElseThrow(() -> new ResourceNotFoundException("Due not found with id: " + dueId));

        DueStatus newStatus = DueStatus.valueOf(status);
        DueStatus oldStatus = due.getStatus();

        due.setStatus(newStatus);

        // If marking as written off or cancelled, adjust customer's total due
        if ((newStatus == DueStatus.WRITTEN_OFF || newStatus == DueStatus.CANCELLED) &&
                oldStatus != DueStatus.PAID && oldStatus != DueStatus.WRITTEN_OFF && oldStatus != DueStatus.CANCELLED) {
            customerService.updateCustomerDue(due.getCustomer().getId(), due.getRemainingAmount().negate());
        }

        CustomerDue updatedDue = dueRepository.save(due);
        log.info("Due status updated successfully for ID: {}", dueId);

        return dueMapper.toResponse(updatedDue);
    }

    @Override
    public void sendDueReminder(Long dueId) {
        log.info("Sending reminder for due ID: {}", dueId);

        CustomerDue due = dueRepository.findById(dueId)
                .orElseThrow(() -> new ResourceNotFoundException("Due not found with id: " + dueId));

        if (due.getStatus() == DueStatus.PAID || due.getStatus() == DueStatus.CANCELLED) {
            throw new BusinessException("Cannot send reminder for paid or cancelled due");
        }

        Customer customer = due.getCustomer();

        // Calculate days overdue or days until due
        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), due.getDueDate());
        String dueMessage;

        if (daysUntilDue < 0) {
            dueMessage = String.format("Your payment of ₹%.2f is %d days overdue. Please pay immediately.",
                    due.getRemainingAmount(), Math.abs(daysUntilDue));
        } else if (daysUntilDue == 0) {
            dueMessage = String.format("Your payment of ₹%.2f is due today.",
                    due.getRemainingAmount());
        } else {
            dueMessage = String.format("Your payment of ₹%.2f is due in %d days.",
                    due.getRemainingAmount(), daysUntilDue);
        }

        String message = String.format(
                "Dear %s,\n\nThis is a reminder regarding your outstanding due.\n" +
                        "Invoice: %s\nDue Date: %s\nAmount Due: ₹%.2f\n\n%s\n\n" +
                        "Please make the payment at your earliest convenience.\n\n" +
                        "Thank you,\nRetail Management Team",
                customer.getName(),
                due.getInvoiceNumber() != null ? due.getInvoiceNumber() : due.getDueReference(),
                due.getDueDate().toString(),
                due.getRemainingAmount(),
                dueMessage
        );

        // Send notifications
        if (customer.getEmail() != null) {
            notificationService.sendEmailNotification(
                    customer.getEmail(),
                    "Payment Reminder - Due Date Approaching",
                    message
            );
        }

        if (customer.getPhone() != null) {
            notificationService.sendSmsNotification(
                    customer.getPhone(),
                    message
            );
        }

        // Create in-app notification
        notificationService.sendInAppNotification(
                customer.getId(),
                message
        );

        // Update reminder count
        due.setReminderCount(due.getReminderCount() + 1);
        due.setLastReminderSent(LocalDateTime.now());
        dueRepository.save(due);

        log.info("Reminder sent successfully for due ID: {}", dueId);
    }

    @Override
    @Scheduled(cron = "0 0 8 * * *") // Every day at 8 AM
    public void processDueReminders() {
        log.info("Processing due reminders at {}", LocalDateTime.now());

        List<CustomerDue> duesNeedingReminder = dueRepository.findDuesNeedingReminder();

        for (CustomerDue due : duesNeedingReminder) {
            try {
                sendDueReminder(due.getId());

                // Add a small delay to avoid overwhelming the system
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("Failed to send reminder for due ID: {}", due.getId(), e);
            }
        }

        log.info("Processed {} due reminders", duesNeedingReminder.size());
    }

    @Override
    public BigDecimal getTotalDueAmount() {
        return dueRepository.getTotalDueAmount();
    }

    @Override
    public BigDecimal getTotalOverdueAmount() {
        return dueRepository.getTotalOverdueAmount(LocalDate.now());
    }

    @Override
    public Long getOverdueCount() {
        return (long) dueRepository.findOverdueDues(LocalDate.now()).size();
    }

    @Override
    public List<CustomerDueResponse> getDuesNeedingReminder() {
        return dueRepository.findDuesNeedingReminder().stream()
                .map(dueMapper::toResponse)
                .collect(Collectors.toList());
    }
}