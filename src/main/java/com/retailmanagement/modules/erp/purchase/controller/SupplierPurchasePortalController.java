package com.retailmanagement.modules.erp.purchase.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.purchase.dto.ErpPurchaseDtos;
import com.retailmanagement.modules.erp.purchase.dto.ErpPurchaseResponses;
import com.retailmanagement.modules.erp.purchase.service.ErpPurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/supplier-portal/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Supplier Purchase Portal", description = "Supplier-facing purchase order acknowledgement and dispatch notice endpoints")
public class SupplierPurchasePortalController {

    private final ErpPurchaseService erpPurchaseService;

    @GetMapping("/{accessToken}")
    @Operation(summary = "Get supplier-facing purchase order view")
    public ErpApiResponse<ErpPurchaseResponses.SupplierPortalPurchaseOrderResponse> getPurchaseOrder(@PathVariable String accessToken) {
        return ErpApiResponse.ok(erpPurchaseService.getSupplierPortalPurchaseOrder(accessToken));
    }

    @PostMapping("/{accessToken}/dispatch-notices")
    @Operation(summary = "Submit supplier dispatch notice for purchase order")
    public ErpApiResponse<ErpPurchaseResponses.SupplierDispatchNoticeResponse> createDispatchNotice(
            @PathVariable String accessToken,
            @RequestBody @Valid ErpPurchaseDtos.CreateSupplierDispatchNoticeRequest request
    ) {
        return ErpApiResponse.ok(erpPurchaseService.createSupplierDispatchNotice(accessToken, request), "Supplier dispatch notice submitted");
    }
}
