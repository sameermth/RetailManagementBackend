package com.retailmanagement.modules.erp.foundation.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.foundation.dto.OrganizationDtos;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/erp/organizations") @RequiredArgsConstructor
@Tag(name = "ERP Organizations", description = "ERP organization management endpoints")
public class OrganizationController {
 private final OrganizationService service;
 @GetMapping @Operation(summary = "List accessible organizations") @PreAuthorize("hasAuthority('org.view')") public ErpApiResponse<List<OrganizationDtos.OrganizationResponse>> list(){ return ErpApiResponse.ok(service.list().stream().map(this::toResponse).toList()); }
 @GetMapping("/{id}") @Operation(summary = "Get organization by id") @PreAuthorize("hasAuthority('org.view')") public ErpApiResponse<OrganizationDtos.OrganizationResponse> get(@PathVariable Long id){ return ErpApiResponse.ok(toResponse(service.get(id))); }
 @PostMapping @Operation(summary = "Create organization") @PreAuthorize("hasAuthority('org.manage')") public ErpApiResponse<OrganizationDtos.OrganizationResponse> create(@Valid @RequestBody OrganizationDtos.CreateOrganizationRequest request){ return ErpApiResponse.ok(toResponse(service.create(toEntity(request))), "ERP organization created"); }
 @PutMapping("/{id}") @Operation(summary = "Update organization") @PreAuthorize("hasAuthority('org.manage')") public ErpApiResponse<OrganizationDtos.OrganizationResponse> update(@PathVariable Long id, @RequestBody OrganizationDtos.UpdateOrganizationRequest request){ return ErpApiResponse.ok(toResponse(service.update(id, request)), "ERP organization updated"); }

 private OrganizationDtos.OrganizationResponse toResponse(Organization organization) {
  return new OrganizationDtos.OrganizationResponse(
          organization.getId(),
          organization.getName(),
          organization.getCode(),
          organization.getLegalName(),
          organization.getPhone(),
          organization.getEmail(),
          organization.getGstin(),
          organization.getOwnerAccountId(),
          organization.getGstThresholdAlertEnabled(),
          organization.getSubscriptionVersion(),
          organization.getIsActive(),
          organization.getCreatedAt(),
          organization.getUpdatedAt()
  );
 }

 private Organization toEntity(OrganizationDtos.CreateOrganizationRequest request) {
  Organization organization = new Organization();
  organization.setName(request.name());
  organization.setCode(request.code());
  organization.setLegalName(request.legalName());
  organization.setPhone(request.phone());
  organization.setEmail(request.email());
  organization.setGstin(request.gstin());
  if (request.gstThresholdAlertEnabled() != null) {
   organization.setGstThresholdAlertEnabled(request.gstThresholdAlertEnabled());
  }
  if (request.isActive() != null) {
   organization.setIsActive(request.isActive());
  }
  return organization;
 }
}
