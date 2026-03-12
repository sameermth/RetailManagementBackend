package com.retailmanagement.modules.purchase.controller;

import com.retailmanagement.modules.purchase.dto.request.SupplierPaymentRequest;
import com.retailmanagement.modules.purchase.dto.response.SupplierPaymentResponse;
import com.retailmanagement.modules.purchase.service.SupplierPaymentService;
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
@RequestMapping("/api/supplier-payments")
@RequiredArgsConstructor
@Tag(name = "Supplier Payments", description = "Supplier payment management endpoints")
public class SupplierPaymentController {

    private final SupplierPaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Create a new supplier payment")
    public ResponseEntity<SupplierPaymentResponse> createPayment(@Valid @RequestBody SupplierPaymentRequest request) {
        return new ResponseEntity<>(paymentService.createPayment(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<SupplierPaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get payment by reference number")
    public ResponseEntity<SupplierPaymentResponse> getPaymentByReference(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.getPaymentByReference(reference));
    }

    @GetMapping
    @Operation(summary = "Get all payments with pagination")
    public ResponseEntity<Page<SupplierPaymentResponse>> getAllPayments(
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAllPayments(pageable));
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Get payments by supplier")
    public ResponseEntity<List<SupplierPaymentResponse>> getPaymentsBySupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(paymentService.getPaymentsBySupplier(supplierId));
    }

    @GetMapping("/purchase/{purchaseId}")
    @Operation(summary = "Get payments by purchase")
    public ResponseEntity<List<SupplierPaymentResponse>> getPaymentsByPurchase(@PathVariable Long purchaseId) {
        return ResponseEntity.ok(paymentService.getPaymentsByPurchase(purchaseId));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get payments by date range")
    public ResponseEntity<List<SupplierPaymentResponse>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(paymentService.getPaymentsByDateRange(startDate, endDate));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
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
}