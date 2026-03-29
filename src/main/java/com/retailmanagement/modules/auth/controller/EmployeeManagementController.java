package com.retailmanagement.modules.auth.controller;

import com.retailmanagement.modules.auth.dto.request.EmployeeManagementRequests;
import com.retailmanagement.modules.auth.dto.response.EmployeeManagementResponses;
import com.retailmanagement.modules.auth.service.EmployeeManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/employees")
@RequiredArgsConstructor
@Tag(name = "ERP Employees", description = "Organization employee and branch allocation endpoints")
public class EmployeeManagementController {

    private final EmployeeManagementService employeeManagementService;

    @GetMapping
    @PreAuthorize("hasAuthority('user.view')")
    @Operation(summary = "List employees for an organization")
    public ResponseEntity<List<EmployeeManagementResponses.EmployeeResponse>> list(@RequestParam Long organizationId) {
        return ResponseEntity.ok(employeeManagementService.list(organizationId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user.view')")
    @Operation(summary = "Get employee by id")
    public ResponseEntity<EmployeeManagementResponses.EmployeeResponse> get(@PathVariable Long id, @RequestParam Long organizationId) {
        return ResponseEntity.ok(employeeManagementService.get(organizationId, id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user.manage')")
    @Operation(summary = "Create employee membership")
    public ResponseEntity<EmployeeManagementResponses.EmployeeResponse> create(@Valid @RequestBody EmployeeManagementRequests.CreateEmployeeRequest request) {
        return ResponseEntity.ok(employeeManagementService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user.manage')")
    @Operation(summary = "Update employee membership")
    public ResponseEntity<EmployeeManagementResponses.EmployeeResponse> update(
            @PathVariable Long id,
            @RequestParam Long organizationId,
            @Valid @RequestBody EmployeeManagementRequests.UpdateEmployeeRequest request
    ) {
        return ResponseEntity.ok(employeeManagementService.update(organizationId, id, request));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('user.manage')")
    @Operation(summary = "Activate employee")
    public ResponseEntity<Void> activate(@PathVariable Long id, @RequestParam Long organizationId) {
        employeeManagementService.activate(organizationId, id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('user.manage')")
    @Operation(summary = "Deactivate employee")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, @RequestParam Long organizationId) {
        employeeManagementService.deactivate(organizationId, id);
        return ResponseEntity.ok().build();
    }
}
