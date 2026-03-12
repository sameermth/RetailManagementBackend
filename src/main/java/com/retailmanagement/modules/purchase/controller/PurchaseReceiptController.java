package com.retailmanagement.modules.purchase.controller;

import com.retailmanagement.modules.purchase.dto.request.PurchaseReceiptRequest;
import com.retailmanagement.modules.purchase.dto.response.PurchaseReceiptResponse;
import com.retailmanagement.modules.purchase.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-receipts")
@RequiredArgsConstructor
@Tag(name = "Purchase Receipts", description = "Purchase receipt management endpoints")
public class PurchaseReceiptController {

    private final PurchaseService purchaseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_MANAGER') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Receive purchase order items")
    public ResponseEntity<PurchaseReceiptResponse> receivePurchase(@Valid @RequestBody PurchaseReceiptRequest request) {
        return new ResponseEntity<>(purchaseService.receivePurchase(request), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PURCHASE_MANAGER')")
    @Operation(summary = "Update payment status for purchase")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam Double paidAmount) {
        purchaseService.updatePaymentStatus(id, paidAmount);
        return ResponseEntity.ok().build();
    }
}