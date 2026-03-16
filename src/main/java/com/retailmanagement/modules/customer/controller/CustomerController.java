package com.retailmanagement.modules.customer.controller;

import com.retailmanagement.modules.customer.dto.request.CustomerRequest;
import com.retailmanagement.modules.customer.dto.response.CustomerResponse;
import com.retailmanagement.modules.customer.dto.response.CustomerSummaryResponse;
import com.retailmanagement.modules.customer.enums.CustomerStatus;
import com.retailmanagement.modules.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management endpoints")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Create a new customer
     * Roles: ADMIN, MANAGER, CASHIER (matching master data)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    @Operation(summary = "Create a new customer")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return new ResponseEntity<>(customerService.createCustomer(request), HttpStatus.CREATED);
    }

    /**
     * Update an existing customer
     * Roles: ADMIN, MANAGER (CASHIER cannot update)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update an existing customer")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    /**
     * Get customer by ID
     * Available to all authenticated users
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    /**
     * Get customer by code
     * Available to all authenticated users
     */
    @GetMapping("/code/{customerCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer by code")
    public ResponseEntity<CustomerResponse> getCustomerByCode(@PathVariable String customerCode) {
        return ResponseEntity.ok(customerService.getCustomerByCode(customerCode));
    }

    /**
     * Get customer by email
     * Available to all authenticated users
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer by email")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(@PathVariable String email) {
        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }

    /**
     * Get customer by phone
     * Available to all authenticated users
     */
    @GetMapping("/phone/{phone}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer by phone")
    public ResponseEntity<CustomerResponse> getCustomerByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(customerService.getCustomerByPhone(phone));
    }

    /**
     * Get all customers with pagination
     * Available to all authenticated users
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all customers with pagination")
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(customerService.getAllCustomers(pageable));
    }

    /**
     * Search customers
     * Available to all authenticated users
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search customers")
    public ResponseEntity<Page<CustomerResponse>> searchCustomers(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(customerService.searchCustomers(q, pageable));
    }

    /**
     * Get customers by status
     * Roles: ADMIN, MANAGER only
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ROLE_ADMIN'') or hasRole('MANAGER')")
    @Operation(summary = "Get customers by status")
    public ResponseEntity<Page<CustomerResponse>> getCustomersByStatus(
            @PathVariable CustomerStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(customerService.getCustomersByStatus(status, pageable));
    }

    /**
     * Delete a customer (soft delete)
     * Roles: ADMIN only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN'')")
    @Operation(summary = "Delete a customer")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activate a customer
     * Roles: ADMIN, MANAGER
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Activate a customer")
    public ResponseEntity<Void> activateCustomer(@PathVariable Long id) {
        customerService.activateCustomer(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Deactivate a customer
     * Roles: ADMIN, MANAGER
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Deactivate a customer")
    public ResponseEntity<Void> deactivateCustomer(@PathVariable Long id) {
        customerService.deactivateCustomer(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Block a customer
     * Roles: ADMIN only
     */
    @PutMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Block a customer")
    public ResponseEntity<Void> blockCustomer(@PathVariable Long id, @RequestParam String reason) {
        customerService.blockCustomer(id, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * Get customers with due amount
     * Roles: ADMIN, MANAGER, ACCOUNTANT
     */
    @GetMapping("/with-due")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get customers with due amount")
    public ResponseEntity<List<CustomerSummaryResponse>> getCustomersWithDue() {
        return ResponseEntity.ok(customerService.getCustomersWithDue());
    }

    /**
     * Get top customers by purchase value
     * Roles: ADMIN, MANAGER
     */
    @GetMapping("/top")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get top customers by purchase value")
    public ResponseEntity<List<CustomerSummaryResponse>> getTopCustomers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(customerService.getTopCustomers(limit));
    }

    /**
     * Get total customer count
     * Available to all authenticated users
     */
    @GetMapping("/stats/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get total customer count")
    public ResponseEntity<Long> getCustomerCount() {
        return ResponseEntity.ok(customerService.getCustomerCount());
    }

    /**
     * Get new customers today
     * Available to all authenticated users
     */
    @GetMapping("/stats/new-today")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get new customers today")
    public ResponseEntity<Long> getNewCustomersToday() {
        return ResponseEntity.ok(customerService.getNewCustomersToday());
    }

    /**
     * Check if email is unique
     * Available to all authenticated users (useful for validation in forms)
     */
    @GetMapping("/check-email")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check if email is unique")
    public ResponseEntity<Boolean> checkEmailUnique(@RequestParam String email) {
        return ResponseEntity.ok(customerService.isEmailUnique(email));
    }

    /**
     * Check if phone is unique
     * Available to all authenticated users (useful for validation in forms)
     */
    @GetMapping("/check-phone")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check if phone is unique")
    public ResponseEntity<Boolean> checkPhoneUnique(@RequestParam String phone) {
        return ResponseEntity.ok(customerService.isPhoneUnique(phone));
    }

    /**
     * Get customer summary (lightweight version for dropdowns)
     * Available to all authenticated users
     */
    @GetMapping("/summaries")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer summaries for dropdowns")
    public ResponseEntity<List<CustomerSummaryResponse>> getCustomerSummaries() {
        return ResponseEntity.ok(customerService.getAllCustomerSummaries());
    }

    /**
     * Bulk import customers (for CSV/Excel upload)
     * Roles: ADMIN, MANAGER
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Bulk import customers")
    public ResponseEntity<List<CustomerResponse>> bulkCreateCustomers(
            @Valid @RequestBody List<CustomerRequest> requests) {
        // This would need to be implemented in service if not exists
        List<CustomerResponse> responses = requests.stream()
                .map(customerService::createCustomer)
                .toList();
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }
}