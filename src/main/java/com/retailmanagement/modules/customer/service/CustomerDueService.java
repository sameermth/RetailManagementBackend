package com.retailmanagement.modules.customer.service;

import com.retailmanagement.modules.customer.dto.request.CustomerDueRequest;
import com.retailmanagement.modules.customer.dto.response.CustomerDueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CustomerDueService {

    CustomerDueResponse createDue(CustomerDueRequest request);

    CustomerDueResponse createDueFromSale(Long saleId);

    CustomerDueResponse getDueById(Long id);

    CustomerDueResponse getDueByReference(String dueReference);

    Page<CustomerDueResponse> getDuesByCustomer(Long customerId, Pageable pageable);

    List<CustomerDueResponse> getOverdueDues();

    List<CustomerDueResponse> getDuesDueBetween(LocalDate startDate, LocalDate endDate);

    CustomerDueResponse recordPayment(Long dueId, BigDecimal paymentAmount, String paymentReference);

    CustomerDueResponse updateDueStatus(Long dueId, String status);

    void sendDueReminder(Long dueId);

    void processDueReminders();

    BigDecimal getTotalDueAmount();

    BigDecimal getTotalOverdueAmount();

    Long getOverdueCount();

    List<CustomerDueResponse> getDuesNeedingReminder();
}