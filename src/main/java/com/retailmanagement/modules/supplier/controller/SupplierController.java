package com.retailmanagement.modules.supplier.controller;

import com.retailmanagement.modules.supplier.dto.request.SupplierRatingRequest;
import com.retailmanagement.modules.supplier.dto.request.SupplierRequest;
import com.retailmanagement.modules.supplier.dto.response.SupplierResponse;
import com.retailmanagement.modules.supplier.dto.response.SupplierSummaryResponse;
import com.retailmanagement.modules.supplier.enums.SupplierStatus;
import com.retailmanagement.modules.supplier.service.SupplierService;
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
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Supplier management endpoints")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Create a new supplier")
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierRequest request) {
        return new ResponseEntity<>(supplierService.createSupplier(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Update an existing supplier")
    public ResponseEntity<SupplierResponse> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID")
    public ResponseEntity<SupplierResponse> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @GetMapping("/code/{supplierCode}")
    @Operation(summary = "Get supplier by code")
    public ResponseEntity<SupplierResponse> getSupplierByCode(@PathVariable String supplierCode) {
        return ResponseEntity.ok(supplierService.getSupplierByCode(supplierCode));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get supplier by email")
    public ResponseEntity<SupplierResponse> getSupplierByEmail(@PathVariable String email) {
        return ResponseEntity.ok(supplierService.getSupplierByEmail(email));
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "Get supplier by phone")
    public ResponseEntity<SupplierResponse> getSupplierByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(supplierService.getSupplierByPhone(phone));
    }

    @GetMapping
    @Operation(summary = "Get all suppliers with pagination")
    public ResponseEntity<Page<SupplierResponse>> getAllSuppliers(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(supplierService.getAllSuppliers(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search suppliers")
    public ResponseEntity<Page<SupplierResponse>> searchSuppliers(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(supplierService.searchSuppliers(q, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get suppliers by status")
    public ResponseEntity<Page<SupplierResponse>> getSuppliersByStatus(
            @PathVariable SupplierStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(supplierService.getSuppliersByStatus(status, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a supplier")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Activate a supplier")
    public ResponseEntity<Void> activateSupplier(@PathVariable Long id) {
        supplierService.activateSupplier(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Deactivate a supplier")
    public ResponseEntity<Void> deactivateSupplier(@PathVariable Long id) {
        supplierService.deactivateSupplier(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Blacklist a supplier")
    public ResponseEntity<Void> blacklistSupplier(@PathVariable Long id, @RequestParam String reason) {
        supplierService.blacklistSupplier(id, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/with-outstanding")
    @Operation(summary = "Get suppliers with outstanding amount")
    public ResponseEntity<List<SupplierSummaryResponse>> getSuppliersWithOutstanding() {
        return ResponseEntity.ok(supplierService.getSuppliersWithOutstanding());
    }

    @GetMapping("/summaries")
    @Operation(summary = "Get all supplier summaries")
    public ResponseEntity<List<SupplierSummaryResponse>> getAllSupplierSummaries() {
        return ResponseEntity.ok(supplierService.getAllSupplierSummaries());
    }

    @PostMapping("/{id}/ratings")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Add rating for a supplier")
    public ResponseEntity<Void> addSupplierRating(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRatingRequest ratingRequest) {
        supplierService.addSupplierRating(id, ratingRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/average-rating")
    @Operation(summary = "Get supplier's average rating")
    public ResponseEntity<Double> getSupplierAverageRating(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierAverageRating(id));
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check if email is unique")
    public ResponseEntity<Boolean> checkEmailUnique(@RequestParam String email) {
        return ResponseEntity.ok(supplierService.isEmailUnique(email));
    }

    @GetMapping("/check-phone")
    @Operation(summary = "Check if phone is unique")
    public ResponseEntity<Boolean> checkPhoneUnique(@RequestParam String phone) {
        return ResponseEntity.ok(supplierService.isPhoneUnique(phone));
    }
}