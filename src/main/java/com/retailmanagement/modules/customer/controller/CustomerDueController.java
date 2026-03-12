package com.retailmanagement.modules.customer.controller;

import com.retailmanagement.modules.customer.dto.request.CustomerDueRequest;
import com.retailmanagement.modules.customer.dto.response.CustomerDueResponse;
import com.retailmanagement.modules.customer.service.CustomerDueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customer-dues")
@RequiredArgsConstructor
@Tag(name = "Customer Dues", description = "Customer due management endpoints")
public class CustomerDueController {

    private final CustomerDueService dueService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    @Operation(summary = "Create a new customer due")
    public ResponseEntity<CustomerDueResponse> createDue(@Valid @RequestBody CustomerDueRequest request) {
        return new ResponseEntity<>(dueService.createDue(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get due by ID")
    public ResponseEntity<CustomerDueResponse> getDueById(@PathVariable Long id) {
        return ResponseEntity.ok(dueService.getDueById(id));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get due by reference number")
    public ResponseEntity<CustomerDueResponse> getDueByReference(@PathVariable String reference) {
        return ResponseEntity.ok(dueService.getDueByReference(reference));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get dues by customer")
    public ResponseEntity<Page<CustomerDueResponse>> getDuesByCustomer(
            @PathVariable Long customerId,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(dueService.getDuesByCustomer(customerId, pageable));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get all overdue dues")
    public ResponseEntity<List<CustomerDueResponse>> getOverdueDues() {
        return ResponseEntity.ok(dueService.getOverdueDues());
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get dues due between dates")
    public ResponseEntity<List<CustomerDueResponse>> getDuesDueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(dueService.getDuesDueBetween(startDate, endDate));
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    @Operation(summary = "Record a payment against a due")
    public ResponseEntity<CustomerDueResponse> recordPayment(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String paymentReference) {
        return ResponseEntity.ok(dueService.recordPayment(id, amount, paymentReference));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update due status")
    public ResponseEntity<CustomerDueResponse> updateDueStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(dueService.updateDueStatus(id, status));
    }

    @PostMapping("/{id}/send-reminder")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Send reminder for a due")
    public ResponseEntity<Void> sendDueReminder(@PathVariable Long id) {
        dueService.sendDueReminder(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/total-due")
    @Operation(summary = "Get total due amount")
    public ResponseEntity<BigDecimal> getTotalDueAmount() {
        return ResponseEntity.ok(dueService.getTotalDueAmount());
    }

    @GetMapping("/stats/total-overdue")
    @Operation(summary = "Get total overdue amount")
    public ResponseEntity<BigDecimal> getTotalOverdueAmount() {
        return ResponseEntity.ok(dueService.getTotalOverdueAmount());
    }

    @GetMapping("/stats/overdue-count")
    @Operation(summary = "Get overdue count")
    public ResponseEntity<Long> getOverdueCount() {
        return ResponseEntity.ok(dueService.getOverdueCount());
    }

    @GetMapping("/needing-reminder")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get dues needing reminder")
    public ResponseEntity<List<CustomerDueResponse>> getDuesNeedingReminder() {
        return ResponseEntity.ok(dueService.getDuesNeedingReminder());
    }
}