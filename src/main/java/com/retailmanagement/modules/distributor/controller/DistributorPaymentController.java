package com.retailmanagement.modules.distributor.controller;

import com.retailmanagement.modules.distributor.dto.request.DistributorPaymentRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorPaymentResponse;
import com.retailmanagement.modules.distributor.service.DistributorPaymentService;
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
@RequestMapping("/api/distributor-payments")
@RequiredArgsConstructor
@Tag(name = "Distributor Payments", description = "Distributor payment management endpoints")
public class DistributorPaymentController {

    private final DistributorPaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALES_MANAGER')")
    @Operation(summary = "Create a new distributor payment")
    public ResponseEntity<DistributorPaymentResponse> createPayment(@Valid @RequestBody DistributorPaymentRequest request) {
        return new ResponseEntity<>(paymentService.createPayment(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<DistributorPaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get payment by reference number")
    public ResponseEntity<DistributorPaymentResponse> getPaymentByReference(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.getPaymentByReference(reference));
    }

    @GetMapping
    @Operation(summary = "Get all payments with pagination")
    public ResponseEntity<Page<DistributorPaymentResponse>> getAllPayments(
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAllPayments(pageable));
    }

    @GetMapping("/distributor/{distributorId}")
    @Operation(summary = "Get payments by distributor")
    public ResponseEntity<List<DistributorPaymentResponse>> getPaymentsByDistributor(@PathVariable Long distributorId) {
        return ResponseEntity.ok(paymentService.getPaymentsByDistributor(distributorId));
    }

    @GetMapping("/distributor/{distributorId}/paged")
    @Operation(summary = "Get payments by distributor with pagination")
    public ResponseEntity<Page<DistributorPaymentResponse>> getPaymentsByDistributorPaged(
            @PathVariable Long distributorId,
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByDistributor(distributorId, pageable));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payments by order")
    public ResponseEntity<List<DistributorPaymentResponse>> getPaymentsByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrder(orderId));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get payments by date range")
    public ResponseEntity<List<DistributorPaymentResponse>> getPaymentsByDateRange(
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

    @GetMapping("/stats/distributor/{distributorId}/total")
    @Operation(summary = "Get total payments by distributor")
    public ResponseEntity<Double> getTotalPaymentsByDistributor(@PathVariable Long distributorId) {
        return ResponseEntity.ok(paymentService.getTotalPaymentsByDistributor(distributorId));
    }
}