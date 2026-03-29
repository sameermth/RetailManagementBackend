package com.retailmanagement.modules.erp.inventory.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.entity.InventoryReservation;
import com.retailmanagement.modules.erp.inventory.service.InventoryReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/inventory-reservations")
@RequiredArgsConstructor
@Validated
@Tag(name = "ERP Inventory Reservations", description = "ERP inventory reservation lifecycle endpoints")
public class InventoryReservationController {

    private final InventoryReservationService inventoryReservationService;

    @GetMapping
    @Operation(summary = "List inventory reservations")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<List<InventoryDtos.InventoryReservationResponse>> list(@RequestParam(required = false) Long organizationId,
                                                                                 @RequestParam(required = false) String status) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(inventoryReservationService.listReservations(orgId, status).stream().map(this::toResponse).toList());
    }

    @PostMapping("/expire")
    @Operation(summary = "Expire active inventory reservations")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public ErpApiResponse<Integer> expire(@RequestParam(required = false) LocalDateTime asOfTime) {
        return ErpApiResponse.ok(inventoryReservationService.expireReservations(asOfTime), "Inventory reservations expired");
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release an active inventory reservation")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public ErpApiResponse<Void> release(@PathVariable Long id,
                                        @RequestBody @Valid ReleaseReservationRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        inventoryReservationService.releaseReservation(orgId, id, request.releaseReason());
        return ErpApiResponse.ok(null, "Inventory reservation released");
    }

    public record ReleaseReservationRequest(
            Long organizationId,
            @NotBlank String releaseReason
    ) {}

    private InventoryDtos.InventoryReservationResponse toResponse(InventoryReservation reservation) {
        return new InventoryDtos.InventoryReservationResponse(
                reservation.getId(),
                reservation.getOrganizationId(),
                reservation.getBranchId(),
                reservation.getWarehouseId(),
                reservation.getProductId(),
                reservation.getBatchId(),
                reservation.getSerialNumberId(),
                reservation.getSourceDocumentType(),
                reservation.getSourceDocumentId(),
                reservation.getSourceDocumentLineId(),
                reservation.getReservedBaseQuantity(),
                reservation.getExpiresAt(),
                reservation.getReleasedAt(),
                reservation.getReleaseReason(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
