package com.retailmanagement.modules.purchase.controller;

import com.retailmanagement.modules.purchase.dto.request.PurchaseRequest;
import com.retailmanagement.modules.purchase.dto.response.PurchaseResponse;
import com.retailmanagement.modules.purchase.dto.response.PurchaseSummaryResponse;
import com.retailmanagement.modules.purchase.enums.PurchaseStatus;
import com.retailmanagement.modules.purchase.service.PurchaseService;
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
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@Tag(name = "Purchases", description = "Purchase order management endpoints")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Create a new purchase order")
    public ResponseEntity<PurchaseResponse> createPurchase(@Valid @RequestBody PurchaseRequest request) {
        return new ResponseEntity<>(purchaseService.createPurchase(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Update an existing purchase order")
    public ResponseEntity<PurchaseResponse> updatePurchase(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.updatePurchase(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order by ID")
    public ResponseEntity<PurchaseResponse> getPurchaseById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id));
    }

    @GetMapping("/order-number/{orderNumber}")
    @Operation(summary = "Get purchase order by order number")
    public ResponseEntity<PurchaseResponse> getPurchaseByOrderNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(purchaseService.getPurchaseByOrderNumber(orderNumber));
    }

    @GetMapping
    @Operation(summary = "Get all purchase orders with pagination")
    public ResponseEntity<Page<PurchaseResponse>> getAllPurchases(
            @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(purchaseService.getAllPurchases(pageable));
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Get purchase orders by supplier")
    public ResponseEntity<List<PurchaseResponse>> getPurchasesBySupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(purchaseService.getPurchasesBySupplier(supplierId));
    }

    @GetMapping("/supplier/{supplierId}/paged")
    @Operation(summary = "Get purchase orders by supplier with pagination")
    public ResponseEntity<Page<PurchaseResponse>> getPurchasesBySupplierPaged(
            @PathVariable Long supplierId,
            @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(purchaseService.getPurchasesBySupplier(supplierId, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get purchase orders by status")
    public ResponseEntity<List<PurchaseResponse>> getPurchasesByStatus(@PathVariable PurchaseStatus status) {
        return ResponseEntity.ok(purchaseService.getPurchasesByStatus(status));
    }

    @GetMapping("/status/{status}/paged")
    @Operation(summary = "Get purchase orders by status with pagination")
    public ResponseEntity<Page<PurchaseResponse>> getPurchasesByStatusPaged(
            @PathVariable PurchaseStatus status,
            @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(purchaseService.getPurchasesByStatus(status, pageable));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get purchase orders by date range")
    public ResponseEntity<List<PurchaseResponse>> getPurchasesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(purchaseService.getPurchasesByDateRange(startDate, endDate));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Approve a purchase order")
    public ResponseEntity<Void> approvePurchase(@PathVariable Long id) {
        purchaseService.approvePurchase(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Cancel a purchase order")
    public ResponseEntity<Void> cancelPurchase(@PathVariable Long id, @RequestParam String reason) {
        purchaseService.cancelPurchase(id, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/total")
    @Operation(summary = "Get total purchase amount for period")
    public ResponseEntity<Double> getTotalPurchaseAmount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(purchaseService.getTotalPurchaseAmount(startDate, endDate));
    }

    @GetMapping("/stats/pending-approval")
    @Operation(summary = "Get count of pending approval purchase orders")
    public ResponseEntity<Long> getPendingApprovalCount() {
        return ResponseEntity.ok(purchaseService.getPendingApprovalCount());
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent purchase orders")
    public ResponseEntity<List<PurchaseSummaryResponse>> getRecentPurchases(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(purchaseService.getRecentPurchases(limit));
    }

    @GetMapping("/check-order-number")
    @Operation(summary = "Check if purchase order number is unique")
    public ResponseEntity<Boolean> checkPurchaseOrderNumberUnique(@RequestParam String orderNumber) {
        return ResponseEntity.ok(purchaseService.isPurchaseOrderNumberUnique(orderNumber));
    }
}