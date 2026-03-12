package com.retailmanagement.modules.sales.controller;

import com.retailmanagement.modules.sales.dto.request.PaymentRequest;
import com.retailmanagement.modules.sales.dto.response.PaymentResponse;
import com.retailmanagement.modules.sales.service.PaymentService;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CASHIER')")
    @Operation(summary = "Create a new payment")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        return new ResponseEntity<>(paymentService.createPayment(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get payment by reference number")
    public ResponseEntity<PaymentResponse> getPaymentByReference(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.getPaymentByReference(reference));
    }

    @GetMapping
    @Operation(summary = "Get all payments with pagination")
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAllPayments(pageable));
    }

    @GetMapping("/sale/{saleId}")
    @Operation(summary = "Get payments by sale")
    public ResponseEntity<List<PaymentResponse>> getPaymentsBySale(@PathVariable Long saleId) {
        return ResponseEntity.ok(paymentService.getPaymentsBySale(saleId));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get payments by date range")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(paymentService.getPaymentsByDateRange(startDate, endDate));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Cancel a payment")
    public ResponseEntity<Void> cancelPayment(@PathVariable Long id, @RequestParam String reason) {
        paymentService.cancelPayment(id, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/total")
    @Operation(summary = "Get total payments for period")
    public ResponseEntity<Double> getTotalPayments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(paymentService.getTotalPaymentsForPeriod(startDate, endDate));
    }

    @GetMapping("/check-transaction")
    @Operation(summary = "Check if transaction ID is unique")
    public ResponseEntity<Boolean> checkTransactionIdUnique(@RequestParam String transactionId) {
        return ResponseEntity.ok(paymentService.isTransactionIdUnique(transactionId));
    }
}