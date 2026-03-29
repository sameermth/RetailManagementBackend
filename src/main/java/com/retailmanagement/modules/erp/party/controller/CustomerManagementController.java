package com.retailmanagement.modules.erp.party.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.party.dto.CustomerDtos;
import com.retailmanagement.modules.erp.party.service.CustomerManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/customers")
@RequiredArgsConstructor
@Tag(name = "ERP Customers", description = "ERP customer relationship and commercial terms endpoints")
public class CustomerManagementController {

    private final CustomerManagementService customerManagementService;

    @GetMapping
    @Operation(summary = "List customers")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<CustomerDtos.CustomerResponse>> listCustomers(@RequestParam Long organizationId) {
        return ErpApiResponse.ok(customerManagementService.listCustomers(organizationId));
    }

    @PostMapping
    @Operation(summary = "Create customer")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<CustomerDtos.CustomerResponse> createCustomer(
            @RequestParam Long organizationId,
            @RequestParam Long branchId,
            @Valid @RequestBody CustomerDtos.UpsertCustomerRequest request
    ) {
        return ErpApiResponse.ok(customerManagementService.createCustomer(organizationId, branchId, request), "Customer created");
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Update customer")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<CustomerDtos.CustomerResponse> updateCustomer(
            @RequestParam Long organizationId,
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerDtos.UpsertCustomerRequest request
    ) {
        return ErpApiResponse.ok(customerManagementService.updateCustomer(organizationId, customerId, request), "Customer updated");
    }

    @GetMapping("/{customerId}/terms")
    @Operation(summary = "Get store-customer terms")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<CustomerDtos.StoreCustomerTermsResponse> getTerms(
            @RequestParam Long organizationId,
            @PathVariable Long customerId
    ) {
        return ErpApiResponse.ok(customerManagementService.getStoreCustomerTerms(organizationId, customerId));
    }

    @PutMapping("/{customerId}/terms")
    @Operation(summary = "Upsert store-customer terms")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<CustomerDtos.StoreCustomerTermsResponse> upsertTerms(
            @RequestParam Long organizationId,
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerDtos.UpsertStoreCustomerTermsRequest request
    ) {
        return ErpApiResponse.ok(
                customerManagementService.upsertStoreCustomerTerms(organizationId, customerId, request),
                "Store customer terms updated"
        );
    }
}
