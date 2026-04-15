package com.retailmanagement.modules.erp.pos.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.pos.dto.PosDtos;
import com.retailmanagement.modules.erp.pos.service.PosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/pos")
@RequiredArgsConstructor
@Tag(name = "ERP POS", description = "Point of sale session, lookup, and quick checkout endpoints")
public class PosController {

    private final PosService posService;

    @GetMapping("/sessions")
    @Operation(summary = "List POS sessions")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<PosDtos.PosSessionSummaryResponse>> listSessions(
            @RequestParam Long organizationId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status
    ) {
        return ErpApiResponse.ok(posService.listSessions(organizationId, branchId, warehouseId, status));
    }

    @GetMapping("/sessions/active")
    @Operation(summary = "Get active POS session for current user")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<PosDtos.PosSessionResponse> activeSession(
            @RequestParam Long organizationId,
            @RequestParam Long branchId,
            @RequestParam Long warehouseId
    ) {
        return ErpApiResponse.ok(posService.getActiveSession(organizationId, branchId, warehouseId));
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get POS session by id")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<PosDtos.PosSessionResponse> getSession(@PathVariable Long id) {
        return ErpApiResponse.ok(posService.getSession(id));
    }

    @PostMapping("/sessions")
    @Operation(summary = "Open POS session")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<PosDtos.PosSessionResponse> openSession(@RequestBody @Valid PosDtos.OpenPosSessionRequest request) {
        return ErpApiResponse.ok(posService.openSession(request), "POS session opened");
    }

    @PostMapping("/sessions/{id}/close")
    @Operation(summary = "Close POS session")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<PosDtos.PosSessionResponse> closeSession(
            @PathVariable Long id,
            @RequestBody(required = false) PosDtos.ClosePosSessionRequest request
    ) {
        return ErpApiResponse.ok(posService.closeSession(id, request), "POS session closed");
    }

    @GetMapping("/sessions/{id}/catalog/lookup")
    @Operation(summary = "Lookup POS sellable product by code, serial, or batch")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<PosDtos.PosCatalogLookupResponse> lookup(
            @PathVariable Long id,
            @RequestParam String query,
            @RequestParam(required = false) Long customerId
    ) {
        return ErpApiResponse.ok(posService.lookup(id, query, customerId));
    }

    @GetMapping("/sessions/{id}/catalog/search")
    @Operation(summary = "Search POS sellable products for cashier type-ahead")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<PosDtos.PosCatalogSearchItemResponse>> searchCatalog(
            @PathVariable Long id,
            @RequestParam String query,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer limit
    ) {
        return ErpApiResponse.ok(posService.searchCatalog(id, query, customerId, limit));
    }

    @PostMapping("/sessions/{id}/checkout")
    @Operation(summary = "Create POS invoice and optional receipt")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<PosDtos.PosCheckoutResponse> checkout(
            @PathVariable Long id,
            @RequestBody @Valid PosDtos.PosCheckoutRequest request
    ) {
        return ErpApiResponse.ok(posService.checkout(id, request), "POS checkout completed");
    }
}
