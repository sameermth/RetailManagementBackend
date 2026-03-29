package com.retailmanagement.modules.erp.foundation.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.foundation.dto.BranchDtos;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
import com.retailmanagement.modules.erp.foundation.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/branches")
@RequiredArgsConstructor
@Tag(name = "ERP Branches", description = "ERP branch management endpoints")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "List branches for an organization")
    @PreAuthorize("hasAuthority('branch.view')")
    public ErpApiResponse<List<BranchDtos.BranchResponse>> list(@RequestParam Long organizationId) {
        return ErpApiResponse.ok(branchService.list(organizationId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get branch by id")
    @PreAuthorize("hasAuthority('branch.view')")
    public ErpApiResponse<BranchDtos.BranchResponse> get(@PathVariable Long id, @RequestParam Long organizationId) {
        return ErpApiResponse.ok(toResponse(branchService.get(organizationId, id)));
    }

    @PostMapping
    @Operation(summary = "Create branch")
    @PreAuthorize("hasAuthority('branch.manage')")
    public ErpApiResponse<BranchDtos.BranchResponse> create(@Valid @RequestBody BranchDtos.CreateBranchRequest request) {
        return ErpApiResponse.ok(toResponse(branchService.create(request)), "ERP branch created");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update branch")
    @PreAuthorize("hasAuthority('branch.manage')")
    public ErpApiResponse<BranchDtos.BranchResponse> update(
            @PathVariable Long id,
            @RequestParam Long organizationId,
            @Valid @RequestBody BranchDtos.UpdateBranchRequest request
    ) {
        return ErpApiResponse.ok(toResponse(branchService.update(organizationId, id, request)), "ERP branch updated");
    }

    private BranchDtos.BranchResponse toResponse(Branch branch) {
        return new BranchDtos.BranchResponse(
                branch.getId(),
                branch.getOrganizationId(),
                branch.getCode(),
                branch.getName(),
                branch.getPhone(),
                branch.getEmail(),
                branch.getAddressLine1(),
                branch.getAddressLine2(),
                branch.getCity(),
                branch.getState(),
                branch.getPostalCode(),
                branch.getCountry(),
                branch.getIsActive(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }
}
