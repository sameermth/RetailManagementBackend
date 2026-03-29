package com.retailmanagement.modules.erp.tax.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.tax.dto.TaxDtos;
import com.retailmanagement.modules.erp.tax.service.TaxRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/tax")
@RequiredArgsConstructor
@Tag(name = "ERP Tax", description = "ERP tax registration and GST threshold endpoints")
public class TaxRegistrationController {

    private final TaxRegistrationService taxRegistrationService;

    @GetMapping("/registrations")
    @Operation(summary = "List tax registrations")
    @PreAuthorize("hasAuthority('tax.view')")
    public ErpApiResponse<TaxDtos.TaxRegistrationListResponse> listRegistrations(
            @RequestParam Long organizationId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) LocalDate documentDate
    ) {
        return ErpApiResponse.ok(taxRegistrationService.listRegistrations(organizationId, branchId, documentDate));
    }

    @PostMapping("/registrations")
    @Operation(summary = "Create tax registration")
    @PreAuthorize("hasAuthority('tax.manage')")
    public ErpApiResponse<TaxDtos.TaxRegistrationResponse> createRegistration(
            @RequestParam Long organizationId,
            @Valid @RequestBody TaxDtos.UpsertTaxRegistrationRequest request
    ) {
        return ErpApiResponse.ok(taxRegistrationService.createRegistration(organizationId, request), "Tax registration created");
    }

    @PutMapping("/registrations/{registrationId}")
    @Operation(summary = "Update tax registration")
    @PreAuthorize("hasAuthority('tax.manage')")
    public ErpApiResponse<TaxDtos.TaxRegistrationResponse> updateRegistration(
            @RequestParam Long organizationId,
            @PathVariable Long registrationId,
            @Valid @RequestBody TaxDtos.UpsertTaxRegistrationRequest request
    ) {
        return ErpApiResponse.ok(taxRegistrationService.updateRegistration(organizationId, registrationId, request), "Tax registration updated");
    }

    @GetMapping("/threshold-status")
    @Operation(summary = "Get GST threshold status")
    @PreAuthorize("hasAuthority('tax.view')")
    public ErpApiResponse<TaxDtos.GstThresholdStatusResponse> thresholdStatus(
            @RequestParam Long organizationId,
            @RequestParam(required = false) LocalDate asOfDate
    ) {
        return ErpApiResponse.ok(taxRegistrationService.thresholdStatus(organizationId, asOfDate));
    }

    @PutMapping("/threshold-settings")
    @Operation(summary = "Update GST threshold settings")
    @PreAuthorize("hasAuthority('tax.manage')")
    public ErpApiResponse<TaxDtos.GstThresholdSettingsResponse> updateThresholdSettings(
            @RequestParam Long organizationId,
            @Valid @RequestBody TaxDtos.GstThresholdSettingsRequest request
    ) {
        return ErpApiResponse.ok(
                taxRegistrationService.updateThresholdSettings(organizationId, request),
                "GST threshold settings updated"
        );
    }
}
