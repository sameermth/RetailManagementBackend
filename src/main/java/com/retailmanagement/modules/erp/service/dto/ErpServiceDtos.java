package com.retailmanagement.modules.erp.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public final class ErpServiceDtos {
    private ErpServiceDtos() {}

    public record CreateServiceTicketRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long customerId,
            Long salesInvoiceId,
            Long salesReturnId,
            @NotBlank String sourceType,
            @NotBlank String priority,
            @NotBlank String complaintSummary,
            String issueDescription,
            LocalDate reportedOn,
            Long assignedToUserId,
            @NotEmpty List<@Valid CreateServiceTicketItemRequest> items
    ) {}

    public record CreateServiceTicketItemRequest(
            @NotNull Long productId,
            Long serialNumberId,
            Long productOwnershipId,
            String symptomNotes
    ) {}

    public record AssignServiceTicketRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long assignedToUserId,
            String remarks
    ) {}

    public record CreateServiceVisitRequest(
            Long organizationId,
            Long branchId,
            Long technicianUserId,
            LocalDateTime scheduledAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            @NotBlank String visitStatus,
            String visitNotes,
            String partsUsedJson,
            String customerFeedback
    ) {}

    public record CloseServiceTicketRequest(
            Long organizationId,
            Long branchId,
            @NotBlank String resolutionStatus,
            String diagnosisNotes,
            String remarks
    ) {}

    public record CreateWarrantyClaimRequest(
            Long organizationId,
            Long branchId,
            Long serviceTicketId,
            @NotNull Long customerId,
            @NotNull Long productId,
            Long serialNumberId,
            Long productOwnershipId,
            Long salesInvoiceId,
            Long salesReturnId,
            Long supplierId,
            Long distributorId,
            String upstreamRouteType,
            String upstreamCompanyName,
            String upstreamReferenceNumber,
            String upstreamStatus,
            LocalDate routedOn,
            @NotBlank String claimType,
            LocalDate claimDate,
            String claimNotes
    ) {}

    public record UpdateWarrantyClaimStatusRequest(
            Long organizationId,
            Long branchId,
            @NotBlank String status,
            LocalDate approvedOn,
            String upstreamRouteType,
            String upstreamCompanyName,
            String upstreamReferenceNumber,
            String upstreamStatus,
            LocalDate routedOn,
            String claimNotes
    ) {}

    public record CreateServiceReplacementRequest(
            Long organizationId,
            Long branchId,
            @NotNull Long warehouseId,
            Long serviceTicketId,
            Long warrantyClaimId,
            Long salesReturnId,
            @NotNull Long customerId,
            @NotNull Long originalProductId,
            Long originalSerialNumberId,
            Long originalProductOwnershipId,
            @NotNull Long replacementProductId,
            Long replacementSerialNumberId,
            @NotNull Long replacementUomId,
            @NotNull @Positive BigDecimal replacementQuantity,
            @NotNull @Positive BigDecimal replacementBaseQuantity,
            @NotBlank String replacementType,
            String stockSourceBucket,
            LocalDate issuedOn,
            LocalDate warrantyStartDate,
            LocalDate warrantyEndDate,
            String notes
    ) {}

    public record ServiceTicketResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long customerId,
            Long salesInvoiceId,
            Long salesReturnId,
            String ticketNumber,
            String sourceType,
            String priority,
            String status,
            String complaintSummary,
            String issueDescription,
            LocalDate reportedOn,
            Long assignedToUserId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ServiceTicketItemResponse(
            Long id,
            Long serviceTicketId,
            Long productId,
            Long serialNumberId,
            Long productOwnershipId,
            String symptomNotes,
            String diagnosisNotes,
            String resolutionStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ServiceVisitResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long serviceTicketId,
            Long technicianUserId,
            LocalDateTime scheduledAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            String visitStatus,
            String visitNotes,
            String partsUsedJson,
            String customerFeedback,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record WarrantyClaimResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long serviceTicketId,
            Long customerId,
            Long productId,
            Long serialNumberId,
            Long productOwnershipId,
            Long salesInvoiceId,
            Long salesReturnId,
            Long supplierId,
            Long distributorId,
            String upstreamRouteType,
            String upstreamCompanyName,
            String upstreamReferenceNumber,
            String upstreamStatus,
            LocalDate routedOn,
            String claimNumber,
            String claimType,
            String status,
            LocalDate claimDate,
            LocalDate approvedOn,
            LocalDate warrantyStartDate,
            LocalDate warrantyEndDate,
            String claimNotes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ServiceReplacementResponse(
            Long id,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long serviceTicketId,
            Long warrantyClaimId,
            Long salesReturnId,
            Long customerId,
            Long originalProductId,
            Long originalSerialNumberId,
            Long originalProductOwnershipId,
            Long replacementProductId,
            Long replacementSerialNumberId,
            Long replacementUomId,
            BigDecimal replacementQuantity,
            BigDecimal replacementBaseQuantity,
            String replacementNumber,
            String replacementType,
            String stockSourceBucket,
            String status,
            LocalDate issuedOn,
            LocalDate warrantyStartDate,
            LocalDate warrantyEndDate,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ServiceTicketDetailsResponse(
            ServiceTicketResponse ticket,
            List<ServiceTicketItemResponse> items,
            List<ServiceVisitResponse> visits
    ) {}
}
