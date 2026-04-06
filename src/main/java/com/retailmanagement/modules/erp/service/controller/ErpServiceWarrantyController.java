package com.retailmanagement.modules.erp.service.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.service.dto.ErpServiceDtos;
import com.retailmanagement.modules.erp.service.entity.ServiceAgreement;
import com.retailmanagement.modules.erp.service.entity.ServiceAgreementItem;
import com.retailmanagement.modules.erp.service.entity.ServiceTicketItem;
import com.retailmanagement.modules.erp.service.entity.ServiceReplacement;
import com.retailmanagement.modules.erp.service.entity.ServiceTicket;
import com.retailmanagement.modules.erp.service.entity.ServiceVisit;
import com.retailmanagement.modules.erp.service.entity.WarrantyClaim;
import com.retailmanagement.modules.erp.service.service.ErpServiceWarrantyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/service")
@RequiredArgsConstructor
@Tag(name = "ERP Service", description = "ERP service ticket and warranty claim endpoints")
public class ErpServiceWarrantyController {

    private final ErpServiceWarrantyService service;

    @GetMapping("/tickets")
    @Operation(summary = "List service tickets")
    @PreAuthorize("hasAuthority('service.view')")
    public ErpApiResponse<List<ErpServiceDtos.ServiceTicketResponse>> listTickets(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(service.listTickets(orgId).stream().map(this::toTicketResponse).toList());
    }

    @GetMapping("/tickets/{id}")
    @Operation(summary = "Get service ticket by id")
    @PreAuthorize("hasAuthority('service.view')")
    public ErpApiResponse<ErpServiceDtos.ServiceTicketDetailsResponse> getTicket(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(toDetailsResponse(service.getTicket(orgId, id)));
    }

    @PostMapping("/tickets")
    @Operation(summary = "Create service ticket")
    @PreAuthorize("hasAuthority('service.manage')")
    public ErpApiResponse<ErpServiceDtos.ServiceTicketResponse> createTicket(@RequestBody @Valid ErpServiceDtos.CreateServiceTicketRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(toTicketResponse(service.createTicket(orgId, branchId, request)), "Service ticket created");
    }

    @PostMapping("/tickets/{id}/assign")
    @Operation(summary = "Assign service ticket")
    @PreAuthorize("hasAuthority('service.manage')")
    public ErpApiResponse<ErpServiceDtos.ServiceTicketResponse> assignTicket(@PathVariable Long id,
                                                                             @RequestBody @Valid ErpServiceDtos.AssignServiceTicketRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(toTicketResponse(service.assignTicket(orgId, branchId, id, request)), "Service ticket assigned");
    }

    @PostMapping("/tickets/{id}/visits")
    @Operation(summary = "Add service visit")
    @PreAuthorize("hasAuthority('service.manage')")
    public ErpApiResponse<ErpServiceDtos.ServiceVisitResponse> addVisit(@PathVariable Long id,
                                                                        @RequestBody @Valid ErpServiceDtos.CreateServiceVisitRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(toVisitResponse(service.addVisit(orgId, branchId, id, request)), "Service visit added");
    }

    @PostMapping("/tickets/{id}/close")
    @Operation(summary = "Close service ticket")
    @PreAuthorize("hasAuthority('service.manage')")
    public ErpApiResponse<ErpServiceDtos.ServiceTicketResponse> closeTicket(@PathVariable Long id,
                                                                            @RequestBody @Valid ErpServiceDtos.CloseServiceTicketRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(toTicketResponse(service.closeTicket(orgId, branchId, id, request)), "Service ticket closed");
    }

    @GetMapping("/warranty-claims")
    @Operation(summary = "List warranty claims")
    @PreAuthorize("hasAuthority('service.view')")
    public ErpApiResponse<List<ErpServiceDtos.WarrantyClaimResponse>> listClaims(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(service.listClaims(orgId).stream().map(this::toClaimResponse).toList());
    }

    @GetMapping("/agreements")
    @Operation(summary = "List service agreements")
    @PreAuthorize("hasAuthority('service.view')")
    public ErpApiResponse<List<ErpServiceDtos.ServiceAgreementResponse>> listAgreements(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(service.listAgreements(orgId).stream()
                .map(agreement -> toAgreementResponse(agreement, List.of()))
                .toList());
    }

    @GetMapping("/agreements/{id}")
    @Operation(summary = "Get service agreement by id")
    @PreAuthorize("hasAuthority('service.view')")
    public ErpApiResponse<ErpServiceDtos.ServiceAgreementResponse> getAgreement(@PathVariable Long id,
                                                                                @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        ErpServiceWarrantyService.ServiceAgreementDetails details = service.getAgreement(orgId, id);
        return ErpApiResponse.ok(toAgreementResponse(details.agreement(), details.items()));
    }

    @PostMapping("/agreements")
    @Operation(summary = "Create service agreement")
    @PreAuthorize("hasAuthority('service.manage')")
    public ErpApiResponse<ErpServiceDtos.ServiceAgreementResponse> createAgreement(@RequestBody @Valid ErpServiceDtos.CreateServiceAgreementRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        ServiceAgreement agreement = service.createAgreement(orgId, branchId, request);
        ErpServiceWarrantyService.ServiceAgreementDetails details = service.getAgreement(orgId, agreement.getId());
        return ErpApiResponse.ok(toAgreementResponse(details.agreement(), details.items()), "Service agreement created");
    }

    @GetMapping("/warranty-claims/{id}")
    @Operation(summary = "Get warranty claim by id")
    @PreAuthorize("hasAuthority('service.view')")
    public ErpApiResponse<ErpServiceDtos.WarrantyClaimResponse> getClaim(@PathVariable Long id,
                                                                         @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(toClaimResponse(service.getClaim(orgId, id)));
    }

    @PostMapping("/warranty-claims")
    @Operation(summary = "Create warranty claim")
    @PreAuthorize("hasAuthority('service.manage')")
    public ErpApiResponse<ErpServiceDtos.WarrantyClaimResponse> createClaim(@RequestBody @Valid ErpServiceDtos.CreateWarrantyClaimRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(toClaimResponse(service.createClaim(orgId, branchId, request)), "Warranty claim created");
    }

    @PostMapping("/warranty-claims/{id}/status")
    @Operation(summary = "Update warranty claim status")
    @PreAuthorize("hasAuthority('service.manage')")
    public ErpApiResponse<ErpServiceDtos.WarrantyClaimResponse> updateClaimStatus(@PathVariable Long id,
                                                                                  @RequestBody @Valid ErpServiceDtos.UpdateWarrantyClaimStatusRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(toClaimResponse(service.updateClaimStatus(orgId, branchId, id, request)), "Warranty claim status updated");
    }

    @GetMapping("/ownership/{id}/warranty")
    @Operation(summary = "Get warranty summary for a sold ownership item")
    @PreAuthorize("hasAuthority('service.view')")
    public ErpApiResponse<ErpServiceDtos.OwnershipWarrantySummaryResponse> getOwnershipWarranty(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(service.getOwnershipWarrantySummary(orgId, id));
    }

    @GetMapping("/ownership/{id}/warranty-extensions")
    @Operation(summary = "List warranty extensions for a sold ownership item")
    @PreAuthorize("hasAuthority('service.view')")
    public ErpApiResponse<List<ErpServiceDtos.WarrantyExtensionResponse>> listWarrantyExtensions(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(service.listWarrantyExtensions(orgId, id));
    }

    @PostMapping("/ownership/{id}/warranty-extensions")
    @Operation(summary = "Create a warranty extension for a sold ownership item")
    @PreAuthorize("hasAuthority('service.manage')")
    public ErpApiResponse<ErpServiceDtos.WarrantyExtensionResponse> createWarrantyExtension(
            @PathVariable Long id,
            @RequestBody @Valid ErpServiceDtos.CreateWarrantyExtensionRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(service.createWarrantyExtension(orgId, branchId, id, request), "Warranty extension created");
    }

    @PostMapping("/warranty-extensions/{id}/cancel")
    @Operation(summary = "Cancel a warranty extension")
    @PreAuthorize("hasAuthority('service.manage')")
    public ErpApiResponse<ErpServiceDtos.WarrantyExtensionResponse> cancelWarrantyExtension(
            @PathVariable Long id,
            @RequestBody @Valid ErpServiceDtos.CancelWarrantyExtensionRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(service.cancelWarrantyExtension(orgId, branchId, id, request), "Warranty extension cancelled");
    }

    @GetMapping("/replacements")
    @Operation(summary = "List service replacements")
    @PreAuthorize("hasAuthority('service.view')")
    public ErpApiResponse<List<ErpServiceDtos.ServiceReplacementResponse>> listReplacements(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(service.listReplacements(orgId).stream().map(this::toReplacementResponse).toList());
    }

    @GetMapping("/replacements/{id}")
    @Operation(summary = "Get service replacement by id")
    @PreAuthorize("hasAuthority('service.view')")
    public ErpApiResponse<ErpServiceDtos.ServiceReplacementResponse> getReplacement(@PathVariable Long id,
                                                                                    @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(toReplacementResponse(service.getReplacement(orgId, id)));
    }

    @PostMapping("/replacements")
    @Operation(summary = "Issue a service replacement")
    @PreAuthorize("hasAuthority('service.manage')")
    public ErpApiResponse<ErpServiceDtos.ServiceReplacementResponse> createReplacement(@RequestBody @Valid ErpServiceDtos.CreateServiceReplacementRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(toReplacementResponse(service.createReplacement(orgId, branchId, request)), "Service replacement issued");
    }

    private ErpServiceDtos.ServiceTicketResponse toTicketResponse(ServiceTicket ticket) {
        return new ErpServiceDtos.ServiceTicketResponse(ticket.getId(), ticket.getOrganizationId(), ticket.getBranchId(),
                ticket.getCustomerId(), ticket.getSalesInvoiceId(), ticket.getSalesReturnId(), ticket.getTicketNumber(), ticket.getSourceType(),
                ticket.getPriority(), ticket.getStatus(), ticket.getComplaintSummary(), ticket.getIssueDescription(),
                ticket.getReportedOn(), ticket.getAssignedToUserId(), ticket.getCreatedAt(), ticket.getUpdatedAt());
    }

    private ErpServiceDtos.ServiceTicketItemResponse toItemResponse(Long organizationId, ServiceTicketItem item) {
        return new ErpServiceDtos.ServiceTicketItemResponse(item.getId(), item.getServiceTicketId(), item.getProductId(),
                item.getSerialNumberId(), item.getProductOwnershipId(), item.getSymptomNotes(), item.getDiagnosisNotes(),
                item.getResolutionStatus(),
                resolveWarrantySummary(organizationId, item.getProductOwnershipId()),
                resolveServiceAgreementSummary(organizationId, item.getProductOwnershipId(), null, null),
                item.getCreatedAt(), item.getUpdatedAt());
    }

    private ErpServiceDtos.ServiceVisitResponse toVisitResponse(ServiceVisit visit) {
        return new ErpServiceDtos.ServiceVisitResponse(visit.getId(), visit.getOrganizationId(), visit.getBranchId(),
                visit.getServiceTicketId(), visit.getTechnicianUserId(), visit.getScheduledAt(), visit.getStartedAt(),
                visit.getCompletedAt(), visit.getVisitStatus(), visit.getVisitNotes(), visit.getPartsUsedJson(),
                visit.getCustomerFeedback(), visit.getCreatedAt(), visit.getUpdatedAt());
    }

    private ErpServiceDtos.WarrantyClaimResponse toClaimResponse(WarrantyClaim claim) {
        return new ErpServiceDtos.WarrantyClaimResponse(claim.getId(), claim.getOrganizationId(), claim.getBranchId(),
                claim.getServiceTicketId(), claim.getCustomerId(), claim.getProductId(), claim.getSerialNumberId(),
                claim.getProductOwnershipId(), claim.getSalesInvoiceId(), claim.getSalesReturnId(), claim.getSupplierId(),
                claim.getDistributorId(), claim.getUpstreamRouteType(), claim.getUpstreamCompanyName(),
                claim.getUpstreamReferenceNumber(), claim.getUpstreamStatus(), claim.getRoutedOn(),
                claim.getClaimNumber(), claim.getClaimType(), claim.getStatus(),
                claim.getClaimDate(), claim.getApprovedOn(), claim.getWarrantyStartDate(), claim.getWarrantyEndDate(),
                resolveWarrantySummary(claim.getOrganizationId(), claim.getProductOwnershipId()),
                resolveServiceAgreementSummary(claim.getOrganizationId(), claim.getProductOwnershipId(), claim.getSalesInvoiceId(), null),
                claim.getClaimNotes(), claim.getCreatedAt(), claim.getUpdatedAt());
    }

    private ErpServiceDtos.ServiceTicketDetailsResponse toDetailsResponse(ErpServiceWarrantyService.ServiceTicketDetails details) {
        return new ErpServiceDtos.ServiceTicketDetailsResponse(
                toTicketResponse(details.ticket()),
                details.items().stream().map(item -> toItemResponse(details.ticket().getOrganizationId(), item)).toList(),
                details.visits().stream().map(this::toVisitResponse).toList()
        );
    }

    private ErpServiceDtos.ServiceReplacementResponse toReplacementResponse(ServiceReplacement replacement) {
        return new ErpServiceDtos.ServiceReplacementResponse(
                replacement.getId(),
                replacement.getOrganizationId(),
                replacement.getBranchId(),
                replacement.getWarehouseId(),
                replacement.getServiceTicketId(),
                replacement.getWarrantyClaimId(),
                replacement.getSalesReturnId(),
                replacement.getCustomerId(),
                replacement.getOriginalProductId(),
                replacement.getOriginalSerialNumberId(),
                replacement.getOriginalProductOwnershipId(),
                replacement.getReplacementProductId(),
                replacement.getReplacementSerialNumberId(),
                replacement.getReplacementUomId(),
                replacement.getReplacementQuantity(),
                replacement.getReplacementBaseQuantity(),
                replacement.getReplacementNumber(),
                replacement.getReplacementType(),
                replacement.getStockSourceBucket(),
                replacement.getStatus(),
                replacement.getIssuedOn(),
                replacement.getWarrantyStartDate(),
                replacement.getWarrantyEndDate(),
                replacement.getNotes(),
                replacement.getCreatedAt(),
                replacement.getUpdatedAt()
        );
    }

    private ErpServiceDtos.ServiceAgreementResponse toAgreementResponse(ServiceAgreement agreement, List<ServiceAgreementItem> items) {
        return service.toServiceAgreementResponse(agreement, items);
    }

    private ErpServiceDtos.OwnershipWarrantySummaryResponse resolveWarrantySummary(Long organizationId, Long ownershipId) {
        if (organizationId == null || ownershipId == null) {
            return null;
        }
        return service.getOwnershipWarrantySummary(organizationId, ownershipId);
    }

    private ErpServiceDtos.ServiceAgreementSummaryResponse resolveServiceAgreementSummary(Long organizationId,
                                                                                          Long ownershipId,
                                                                                          Long salesInvoiceId,
                                                                                          Long salesInvoiceLineId) {
        if (organizationId == null) {
            return null;
        }
        return service.resolveServiceAgreementSummary(organizationId, ownershipId, salesInvoiceId, salesInvoiceLineId);
    }
}
