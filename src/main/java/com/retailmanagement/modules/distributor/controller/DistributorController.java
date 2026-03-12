package com.retailmanagement.modules.distributor.controller;

import com.retailmanagement.modules.distributor.dto.request.DistributorRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorResponse;
import com.retailmanagement.modules.distributor.dto.response.DistributorSummaryResponse;
import com.retailmanagement.modules.distributor.enums.DistributorStatus;
import com.retailmanagement.modules.distributor.service.DistributorService;
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
@RequestMapping("/api/distributors")
@RequiredArgsConstructor
@Tag(name = "Distributors", description = "Distributor management endpoints")
public class DistributorController {

    private final DistributorService distributorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALES_MANAGER')")
    @Operation(summary = "Create a new distributor")
    public ResponseEntity<DistributorResponse> createDistributor(@Valid @RequestBody DistributorRequest request) {
        return new ResponseEntity<>(distributorService.createDistributor(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALES_MANAGER')")
    @Operation(summary = "Update an existing distributor")
    public ResponseEntity<DistributorResponse> updateDistributor(
            @PathVariable Long id,
            @Valid @RequestBody DistributorRequest request) {
        return ResponseEntity.ok(distributorService.updateDistributor(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get distributor by ID")
    public ResponseEntity<DistributorResponse> getDistributorById(@PathVariable Long id) {
        return ResponseEntity.ok(distributorService.getDistributorById(id));
    }

    @GetMapping("/code/{distributorCode}")
    @Operation(summary = "Get distributor by code")
    public ResponseEntity<DistributorResponse> getDistributorByCode(@PathVariable String distributorCode) {
        return ResponseEntity.ok(distributorService.getDistributorByCode(distributorCode));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get distributor by email")
    public ResponseEntity<DistributorResponse> getDistributorByEmail(@PathVariable String email) {
        return ResponseEntity.ok(distributorService.getDistributorByEmail(email));
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "Get distributor by phone")
    public ResponseEntity<DistributorResponse> getDistributorByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(distributorService.getDistributorByPhone(phone));
    }

    @GetMapping
    @Operation(summary = "Get all distributors with pagination")
    public ResponseEntity<Page<DistributorResponse>> getAllDistributors(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(distributorService.getAllDistributors(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search distributors")
    public ResponseEntity<Page<DistributorResponse>> searchDistributors(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(distributorService.searchDistributors(q, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get distributors by status")
    public ResponseEntity<Page<DistributorResponse>> getDistributorsByStatus(
            @PathVariable DistributorStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(distributorService.getDistributorsByStatus(status, pageable));
    }

    @GetMapping("/region/{region}")
    @Operation(summary = "Get distributors by region")
    public ResponseEntity<List<DistributorResponse>> getDistributorsByRegion(@PathVariable String region) {
        return ResponseEntity.ok(distributorService.getDistributorsByRegion(region));
    }

    @GetMapping("/territory/{territory}")
    @Operation(summary = "Get distributors by territory")
    public ResponseEntity<List<DistributorResponse>> getDistributorsByTerritory(@PathVariable String territory) {
        return ResponseEntity.ok(distributorService.getDistributorsByTerritory(territory));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a distributor")
    public ResponseEntity<Void> deleteDistributor(@PathVariable Long id) {
        distributorService.deleteDistributor(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALES_MANAGER')")
    @Operation(summary = "Activate a distributor")
    public ResponseEntity<Void> activateDistributor(@PathVariable Long id) {
        distributorService.activateDistributor(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALES_MANAGER')")
    @Operation(summary = "Deactivate a distributor")
    public ResponseEntity<Void> deactivateDistributor(@PathVariable Long id) {
        distributorService.deactivateDistributor(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Blacklist a distributor")
    public ResponseEntity<Void> blacklistDistributor(@PathVariable Long id, @RequestParam String reason) {
        distributorService.blacklistDistributor(id, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/with-outstanding")
    @Operation(summary = "Get distributors with outstanding amount")
    public ResponseEntity<List<DistributorSummaryResponse>> getDistributorsWithOutstanding() {
        return ResponseEntity.ok(distributorService.getDistributorsWithOutstanding());
    }

    @GetMapping("/summaries")
    @Operation(summary = "Get all distributor summaries")
    public ResponseEntity<List<DistributorSummaryResponse>> getAllDistributorSummaries() {
        return ResponseEntity.ok(distributorService.getAllDistributorSummaries());
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check if email is unique")
    public ResponseEntity<Boolean> checkEmailUnique(@RequestParam String email) {
        return ResponseEntity.ok(distributorService.isEmailUnique(email));
    }

    @GetMapping("/check-phone")
    @Operation(summary = "Check if phone is unique")
    public ResponseEntity<Boolean> checkPhoneUnique(@RequestParam String phone) {
        return ResponseEntity.ok(distributorService.isPhoneUnique(phone));
    }
}