package com.retailmanagement.modules.sales.controller;

import com.retailmanagement.modules.sales.dto.request.SaleRequest;
import com.retailmanagement.modules.sales.dto.response.SaleResponse;
import com.retailmanagement.modules.sales.dto.response.SaleSummaryResponse;
import com.retailmanagement.modules.sales.enums.SaleStatus;
import com.retailmanagement.modules.sales.service.SalesService;
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
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Tag(name = "Sales", description = "Sales management endpoints")
public class SalesController {

    private final SalesService salesService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CASHIER')")
    @Operation(summary = "Create a new sale")
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody SaleRequest request) {
        return new ResponseEntity<>(salesService.createSale(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CASHIER')")
    @Operation(summary = "Update an existing sale")
    public ResponseEntity<SaleResponse> updateSale(
            @PathVariable Long id,
            @Valid @RequestBody SaleRequest request) {
        return ResponseEntity.ok(salesService.updateSale(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sale by ID")
    public ResponseEntity<SaleResponse> getSaleById(@PathVariable Long id) {
        return ResponseEntity.ok(salesService.getSaleById(id));
    }

    @GetMapping("/invoice/{invoiceNumber}")
    @Operation(summary = "Get sale by invoice number")
    public ResponseEntity<SaleResponse> getSaleByInvoiceNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(salesService.getSaleByInvoiceNumber(invoiceNumber));
    }

    @GetMapping
    @Operation(summary = "Get all sales with pagination")
    public ResponseEntity<Page<SaleResponse>> getAllSales(
            @PageableDefault(size = 20, sort = "saleDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(salesService.getAllSales(pageable));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get sales by customer")
    public ResponseEntity<List<SaleResponse>> getSalesByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(salesService.getSalesByCustomer(customerId));
    }

    @GetMapping("/customer/{customerId}/paged")
    @Operation(summary = "Get sales by customer with pagination")
    public ResponseEntity<Page<SaleResponse>> getSalesByCustomerPaged(
            @PathVariable Long customerId,
            @PageableDefault(size = 20, sort = "saleDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(salesService.getSalesByCustomer(customerId, pageable));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get sales by date range")
    public ResponseEntity<List<SaleResponse>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(salesService.getSalesByDateRange(startDate, endDate));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get sales by status")
    public ResponseEntity<List<SaleResponse>> getSalesByStatus(@PathVariable SaleStatus status) {
        return ResponseEntity.ok(salesService.getSalesByStatus(status));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Cancel a sale")
    public ResponseEntity<Void> cancelSale(@PathVariable Long id, @RequestParam String reason) {
        salesService.cancelSale(id, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Process a return")
    public ResponseEntity<SaleResponse> processReturn(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestBody(required = false) List<Long> itemIds) {
        return ResponseEntity.ok(salesService.processReturn(id, reason, itemIds));
    }

    @GetMapping("/stats/total")
    @Operation(summary = "Get total sales for period")
    public ResponseEntity<Double> getTotalSales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(salesService.getTotalSales(startDate, endDate));
    }

    @GetMapping("/stats/count")
    @Operation(summary = "Get sales count for period")
    public ResponseEntity<Long> getSalesCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(salesService.getSalesCount(startDate, endDate));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent sales")
    public ResponseEntity<List<SaleSummaryResponse>> getRecentSales(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(salesService.getRecentSales(limit));
    }

    @GetMapping("/check-invoice")
    @Operation(summary = "Check if invoice number is unique")
    public ResponseEntity<Boolean> checkInvoiceNumberUnique(@RequestParam String invoiceNumber) {
        return ResponseEntity.ok(salesService.isInvoiceNumberUnique(invoiceNumber));
    }
}