package com.retailmanagement.modules.erp.inventory.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.service.StockCountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/inventory-counts")
@RequiredArgsConstructor
@Tag(name = "ERP Inventory Counts", description = "Stock count session and variance posting endpoints")
public class StockCountController {

    private final StockCountService stockCountService;

    @GetMapping
    @Operation(summary = "List stock count sessions")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<java.util.List<InventoryDtos.StockCountSessionResponse>> list(
            @RequestParam Long organizationId,
            @RequestParam(required = false) Long warehouseId
    ) {
        return ErpApiResponse.ok(stockCountService.listSessions(organizationId, warehouseId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock count session details")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ErpApiResponse<InventoryDtos.StockCountSessionResponse> get(@PathVariable Long id) {
        return ErpApiResponse.ok(stockCountService.getSession(id));
    }

    @PostMapping
    @Operation(summary = "Create stock count session")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public ErpApiResponse<InventoryDtos.StockCountSessionResponse> create(
            @RequestBody @Valid InventoryDtos.CreateStockCountSessionRequest request
    ) {
        return ErpApiResponse.ok(stockCountService.createSession(request), "Stock count session created");
    }

    @PutMapping("/{id}/lines")
    @Operation(summary = "Upsert stock count lines")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public ErpApiResponse<InventoryDtos.StockCountSessionResponse> upsertLines(
            @PathVariable Long id,
            @RequestBody @Valid InventoryDtos.UpsertStockCountLinesRequest request
    ) {
        return ErpApiResponse.ok(stockCountService.upsertLines(id, request), "Stock count lines saved");
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit stock count for variance review")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public ErpApiResponse<InventoryDtos.StockCountSessionResponse> submit(@PathVariable Long id) {
        return ErpApiResponse.ok(stockCountService.submitSession(id), "Stock count session submitted");
    }

    @PostMapping("/{id}/post-variances")
    @Operation(summary = "Post stock count variances through stock adjustments")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public ErpApiResponse<InventoryDtos.StockCountSessionResponse> postVariances(@PathVariable Long id) {
        return ErpApiResponse.ok(stockCountService.postVariances(id), "Stock count variances posted");
    }
}
