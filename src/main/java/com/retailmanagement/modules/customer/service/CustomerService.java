package com.retailmanagement.modules.customer.service;

import com.retailmanagement.modules.customer.dto.request.CustomerRequest;
import com.retailmanagement.modules.customer.dto.response.CustomerResponse;
import com.retailmanagement.modules.customer.dto.response.CustomerSummaryResponse;
import com.retailmanagement.modules.customer.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse updateCustomer(Long id, CustomerRequest request);

    CustomerResponse getCustomerById(Long id);

    CustomerResponse getCustomerByCode(String customerCode);

    CustomerResponse getCustomerByEmail(String email);

    CustomerResponse getCustomerByPhone(String phone);

    Page<CustomerResponse> getAllCustomers(Pageable pageable);

    Page<CustomerResponse> searchCustomers(String searchTerm, Pageable pageable);

    Page<CustomerResponse> getCustomersByStatus(CustomerStatus status, Pageable pageable);

    void deleteCustomer(Long id);

    void activateCustomer(Long id);

    void deactivateCustomer(Long id);

    void blockCustomer(Long id, String reason);

    CustomerResponse updateCustomerDue(Long id, BigDecimal amount);

    List<CustomerSummaryResponse> getCustomersWithDue();

    List<CustomerSummaryResponse> getTopCustomers(int limit);

    long getCustomerCount();

    long getNewCustomersToday();

    boolean isEmailUnique(String email);

    boolean isPhoneUnique(String phone);

    void updateLoyaltyPoints(Long customerId, int points, String transactionType, Long saleId);

    String determineLoyaltyTier(BigDecimal totalPurchaseAmount);

    List<CustomerSummaryResponse> getAllCustomerSummaries();
}