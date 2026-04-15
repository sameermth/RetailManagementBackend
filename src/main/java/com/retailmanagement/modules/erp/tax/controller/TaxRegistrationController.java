package com.retailmanagement.modules.erp.tax.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.tax.dto.TaxDtos;
import com.retailmanagement.modules.erp.tax.service.GstinLookupService;
import com.retailmanagement.modules.erp.tax.service.TaxComplianceService;
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
    private final GstinLookupService gstinLookupService;
    private final TaxComplianceService taxComplianceService;

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

    @GetMapping("/gstin-lookup")
    @Operation(summary = "Lookup GST business details by GSTIN")
    @PreAuthorize("hasAuthority('tax.view')")
    public ErpApiResponse<TaxDtos.GstinLookupResponse> lookupGstin(@RequestParam String gstin) {
        return ErpApiResponse.ok(gstinLookupService.lookup(gstin));
    }

    @GetMapping("/compliance/invoices/{invoiceId}/documents")
    @Operation(summary = "List GST compliance drafts for a sales invoice")
    @PreAuthorize("hasAuthority('tax.view')")
    public ErpApiResponse<java.util.List<TaxDtos.TaxComplianceDocumentSummaryResponse>> listInvoiceComplianceDocuments(@PathVariable Long invoiceId) {
        return ErpApiResponse.ok(taxComplianceService.listInvoiceDocuments(invoiceId));
    }

    @GetMapping("/compliance/documents/{documentId}")
    @Operation(summary = "Get GST compliance document")
    @PreAuthorize("hasAuthority('tax.view')")
    public ErpApiResponse<TaxDtos.TaxComplianceDocumentResponse> getComplianceDocument(@PathVariable Long documentId) {
        return ErpApiResponse.ok(taxComplianceService.getDocument(documentId));
    }

    @PostMapping("/compliance/invoices/{invoiceId}/drafts/e-invoice")
    @Operation(summary = "Create e-invoice draft payload from a sales invoice")
    @PreAuthorize("hasAuthority('tax.manage')")
    public ErpApiResponse<TaxDtos.TaxComplianceDocumentResponse> createEinvoiceDraft(
            @PathVariable Long invoiceId,
            @RequestBody(required = false) TaxDtos.TaxComplianceDraftRequest request
    ) {
        return ErpApiResponse.ok(taxComplianceService.createEinvoiceDraft(invoiceId, request), "E-invoice draft generated");
    }

    @PostMapping("/compliance/invoices/{invoiceId}/drafts/e-way-bill")
    @Operation(summary = "Create e-way bill draft payload from a sales invoice")
    @PreAuthorize("hasAuthority('tax.manage')")
    public ErpApiResponse<TaxDtos.TaxComplianceDocumentResponse> createEwayBillDraft(
            @PathVariable Long invoiceId,
            @RequestBody(required = false) TaxDtos.TaxComplianceDraftRequest request
    ) {
        return ErpApiResponse.ok(taxComplianceService.createEwayBillDraft(invoiceId, request), "E-way bill draft generated");
    }

    @PostMapping("/compliance/documents/{documentId}/submit")
    @Operation(summary = "Submit GST compliance document to configured provider")
    @PreAuthorize("hasAuthority('tax.manage')")
    public ErpApiResponse<TaxDtos.TaxComplianceDocumentResponse> submitComplianceDocument(@PathVariable Long documentId) {
        return ErpApiResponse.ok(taxComplianceService.submitDocument(documentId), "GST compliance document submitted");
    }

    @PostMapping("/compliance/documents/{documentId}/sync-status")
    @Operation(summary = "Sync GST compliance document status from configured provider")
    @PreAuthorize("hasAuthority('tax.manage')")
    public ErpApiResponse<TaxDtos.TaxComplianceDocumentResponse> syncComplianceDocument(@PathVariable Long documentId) {
        return ErpApiResponse.ok(taxComplianceService.syncDocumentStatus(documentId), "GST compliance status synchronized");
    }
}
