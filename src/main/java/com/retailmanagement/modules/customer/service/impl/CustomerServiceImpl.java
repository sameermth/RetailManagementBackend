package com.retailmanagement.modules.customer.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.customer.dto.request.CustomerRequest;
import com.retailmanagement.modules.customer.dto.response.CustomerResponse;
import com.retailmanagement.modules.customer.dto.response.CustomerSummaryResponse;
import com.retailmanagement.modules.customer.enums.CustomerStatus;
import com.retailmanagement.modules.customer.mapper.CustomerMapper;
import com.retailmanagement.modules.customer.model.Customer;
import com.retailmanagement.modules.customer.model.LoyaltyTransaction;
import com.retailmanagement.modules.customer.repository.CustomerRepository;
import com.retailmanagement.modules.customer.repository.LoyaltyTransactionRepository;
import com.retailmanagement.modules.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Creating new customer with name: {}", request.getName());

        // Check if email already exists
        if (request.getEmail() != null && customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Customer with email " + request.getEmail() + " already exists");
        }

        // Check if phone already exists
        if (request.getPhone() != null && customerRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Customer with phone " + request.getPhone() + " already exists");
        }

        Customer customer = customerMapper.toEntity(request);

        // Generate customer code
        customer.setCustomerCode(generateCustomerCode());
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setLoyaltyPoints(0);
        customer.setLoyaltyTier("BRONZE");
        customer.setTotalPurchaseAmount(BigDecimal.ZERO);
        customer.setCreatedBy("SYSTEM"); // In real app, get from SecurityContext
        customer.setUpdatedBy("SYSTEM");

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer created successfully with code: {}", savedCustomer.getCustomerCode());

        return customerMapper.toResponse(savedCustomer);
    }

    private String generateCustomerCode() {
        String year = String.valueOf(LocalDate.now().getYear()).substring(2);
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String customerCode = "CUST-" + year + "-" + randomPart;

        while (customerRepository.existsByCustomerCode(customerCode)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            customerCode = "CUST-" + year + "-" + randomPart;
        }

        return customerCode;
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        log.info("Updating customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail()) &&
                customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Customer with email " + request.getEmail() + " already exists");
        }

        // Check phone uniqueness if changed
        if (request.getPhone() != null && !request.getPhone().equals(customer.getPhone()) &&
                customerRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Customer with phone " + request.getPhone() + " already exists");
        }

        // Update fields
        customer.setName(request.getName());
        customer.setCustomerType(request.getCustomerType());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAlternatePhone(request.getAlternatePhone());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setCountry(request.getCountry());
        customer.setPincode(request.getPincode());
        customer.setGstNumber(request.getGstNumber());
        customer.setPanNumber(request.getPanNumber());
        customer.setWebsite(request.getWebsite());
        customer.setBusinessName(request.getBusinessName());
        customer.setContactPerson(request.getContactPerson());
        customer.setDesignation(request.getDesignation());
        customer.setCreditLimit(request.getCreditLimit());
        customer.setPaymentTerms(request.getPaymentTerms());
        customer.setDueReminderEnabled(request.getDueReminderEnabled());
        customer.setReminderFrequencyDays(request.getReminderFrequencyDays());
        customer.setNotes(request.getNotes());
        customer.setIsActive(request.getIsActive());
        customer.setUpdatedBy("SYSTEM");

        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Customer updated successfully with ID: {}", updatedCustomer.getId());

        return customerMapper.toResponse(updatedCustomer);
    }

    @Override
    public CustomerResponse getCustomerById(Long id) {
        log.debug("Fetching customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        return customerMapper.toResponse(customer);
    }

    @Override
    public CustomerResponse getCustomerByCode(String customerCode) {
        log.debug("Fetching customer with code: {}", customerCode);

        Customer customer = customerRepository.findByCustomerCode(customerCode)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with code: " + customerCode));

        return customerMapper.toResponse(customer);
    }

    @Override
    public CustomerResponse getCustomerByEmail(String email) {
        log.debug("Fetching customer with email: {}", email);

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

        return customerMapper.toResponse(customer);
    }

    @Override
    public CustomerResponse getCustomerByPhone(String phone) {
        log.debug("Fetching customer with phone: {}", phone);

        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with phone: " + phone));

        return customerMapper.toResponse(customer);
    }

    @Override
    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        log.debug("Fetching all customers with pagination");

        return customerRepository.findAll(pageable)
                .map(customerMapper::toResponse);
    }

    @Override
    public Page<CustomerResponse> searchCustomers(String searchTerm, Pageable pageable) {
        log.debug("Searching customers with term: {}", searchTerm);

        return customerRepository.searchCustomers(searchTerm, pageable)
                .map(customerMapper::toResponse);
    }

    @Override
    public Page<CustomerResponse> getCustomersByStatus(CustomerStatus status, Pageable pageable) {
        log.debug("Fetching customers with status: {}", status);

        return customerRepository.findByStatus(status, pageable)
                .map(customerMapper::toResponse);
    }

    @Override
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        // Check if customer has any sales
        if (customer.getSales() != null && !customer.getSales().isEmpty()) {
            throw new BusinessException("Cannot delete customer with existing sales records");
        }

        customerRepository.delete(customer);
        log.info("Customer deleted successfully with ID: {}", id);
    }

    @Override
    public void activateCustomer(Long id) {
        log.info("Activating customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setUpdatedBy("SYSTEM");
        customerRepository.save(customer);
    }

    @Override
    public void deactivateCustomer(Long id) {
        log.info("Deactivating customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setUpdatedBy("SYSTEM");
        customerRepository.save(customer);
    }

    @Override
    public void blockCustomer(Long id, String reason) {
        log.info("Blocking customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        customer.setStatus(CustomerStatus.BLOCKED);
        customer.setNotes(customer.getNotes() + " [Blocked: " + reason + "]");
        customer.setUpdatedBy("SYSTEM");
        customerRepository.save(customer);
    }

    @Override
    public CustomerResponse updateCustomerDue(Long id, BigDecimal amount) {
        log.info("Updating due amount for customer ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        customer.setTotalDueAmount(customer.getTotalDueAmount().add(amount));
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            customer.setLastDueDate(LocalDateTime.now());
        }

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toResponse(updatedCustomer);
    }

    @Override
    public List<CustomerSummaryResponse> getCustomersWithDue() {
        log.debug("Fetching customers with due amount");

        return customerRepository.findCustomersWithDue().stream()
                .map(customerMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerSummaryResponse> getTopCustomers(int limit) {
        log.debug("Fetching top {} customers", limit);

        Pageable pageable = PageRequest.of(0, limit);
        return customerRepository.findTopCustomers(pageable).stream()
                .map(customerMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getCustomerCount() {
        return customerRepository.count();
    }

    @Override
    public long getNewCustomersToday() {
        return customerRepository.countByCreatedDate(LocalDate.now());
    }

    @Override
    public boolean isEmailUnique(String email) {
        return !customerRepository.existsByEmail(email);
    }

    @Override
    public boolean isPhoneUnique(String phone) {
        return !customerRepository.existsByPhone(phone);
    }

    @Override
    @Transactional
    public void updateLoyaltyPoints(Long customerId, int points, String transactionType, Long saleId) {
        log.info("Updating loyalty points for customer ID: {} with {} points", customerId, points);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // Create loyalty transaction
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(customer)
                .transactionReference(generateTransactionReference())
                .transactionType(com.retailmanagement.modules.customer.enums.LoyaltyTransactionType.valueOf(transactionType))
                .points(Math.abs(points))
                .description(transactionType.equals("EARNED") ? "Earned from purchase" : "Redeemed for purchase")
                .saleId(saleId)
                .expiryDate(LocalDateTime.now().plusMonths(6)) // Points expire in 6 months
                .build();

        loyaltyTransactionRepository.save(transaction);

        // Update customer points
        if (transactionType.equals("EARNED")) {
            customer.setLoyaltyPoints(customer.getLoyaltyPoints() + Math.abs(points));
        } else if (transactionType.equals("REDEEMED")) {
            customer.setLoyaltyPoints(customer.getLoyaltyPoints() - Math.abs(points));
        }

        // Update total purchase amount for earned points
        if (transactionType.equals("EARNED") && saleId != null) {
            // In real app, you would fetch the sale amount and add to totalPurchaseAmount
            // customer.setTotalPurchaseAmount(customer.getTotalPurchaseAmount().add(saleAmount));
        }

        // Update loyalty tier based on total purchase amount
        customer.setLoyaltyTier(determineLoyaltyTier(customer.getTotalPurchaseAmount()));

        customerRepository.save(customer);
    }

    private String generateTransactionReference() {
        return "LOY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public String determineLoyaltyTier(BigDecimal totalPurchaseAmount) {
        if (totalPurchaseAmount == null) {
            return "BRONZE";
        }

        double amount = totalPurchaseAmount.doubleValue();

        if (amount >= 100000) {
            return "PLATINUM";
        } else if (amount >= 50000) {
            return "GOLD";
        } else if (amount >= 10000) {
            return "SILVER";
        } else {
            return "BRONZE";
        }
    }
}