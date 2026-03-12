package com.retailmanagement.modules.customer.controller;

import com.retailmanagement.modules.customer.dto.response.LoyaltySummaryResponse;
import com.retailmanagement.modules.customer.model.LoyaltyTransaction;
import com.retailmanagement.modules.customer.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty", description = "Customer loyalty management endpoints")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/customer/{customerId}/summary")
    @Operation(summary = "Get loyalty summary for customer")
    public ResponseEntity<LoyaltySummaryResponse> getCustomerLoyaltySummary(@PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyService.getCustomerLoyaltySummary(customerId));
    }

    @GetMapping("/customer/{customerId}/points")
    @Operation(summary = "Get customer's loyalty points")
    public ResponseEntity<Integer> getCustomerPoints(@PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyService.getCustomerPoints(customerId));
    }

    @GetMapping("/customer/{customerId}/tier")
    @Operation(summary = "Get customer's loyalty tier")
    public ResponseEntity<String> getCustomerTier(@PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyService.getCustomerTier(customerId));
    }

    @PostMapping("/customer/{customerId}/earn")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    @Operation(summary = "Earn loyalty points from purchase")
    public ResponseEntity<LoyaltyTransaction> earnPoints(
            @PathVariable Long customerId,
            @RequestParam BigDecimal purchaseAmount,
            @RequestParam(required = false) Long saleId) {
        return ResponseEntity.ok(loyaltyService.earnPoints(customerId, purchaseAmount, saleId));
    }

    @PostMapping("/customer/{customerId}/redeem")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    @Operation(summary = "Redeem loyalty points")
    public ResponseEntity<LoyaltyTransaction> redeemPoints(
            @PathVariable Long customerId,
            @RequestParam Integer points,
            @RequestParam String redeemedFor,
            @RequestParam(required = false) Long saleId) {
        return ResponseEntity.ok(loyaltyService.redeemPoints(customerId, points, redeemedFor, saleId));
    }

    @PostMapping("/customer/{customerId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust loyalty points (admin only)")
    public ResponseEntity<LoyaltyTransaction> adjustPoints(
            @PathVariable Long customerId,
            @RequestParam Integer points,
            @RequestParam String reason) {
        return ResponseEntity.ok(loyaltyService.adjustPoints(customerId, points, reason));
    }

    @GetMapping("/customer/{customerId}/transactions")
    @Operation(summary = "Get customer's loyalty transaction history")
    public ResponseEntity<List<LoyaltyTransaction>> getCustomerTransactions(@PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyService.getCustomerTransactionHistory(customerId));
    }

    @GetMapping("/customer/{customerId}/recent-transactions")
    @Operation(summary = "Get customer's recent loyalty transactions")
    public ResponseEntity<List<LoyaltySummaryResponse.LoyaltyTransactionResponse>> getRecentTransactions(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(loyaltyService.getRecentTransactions(customerId, limit));
    }

    @GetMapping("/customer/{customerId}/points-to-next-tier")
    @Operation(summary = "Get points needed to reach next tier")
    public ResponseEntity<Integer> getPointsToNextTier(@PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyService.getPointsToNextTier(customerId));
    }

    @GetMapping("/customer/{customerId}/next-tier")
    @Operation(summary = "Get customer's next loyalty tier")
    public ResponseEntity<String> getNextTier(@PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyService.getNextTier(customerId));
    }

    @GetMapping("/customer/{customerId}/points-value")
    @Operation(summary = "Calculate monetary value of points")
    public ResponseEntity<BigDecimal> calculatePointsValue(
            @PathVariable Long customerId,
            @RequestParam Integer points) {
        return ResponseEntity.ok(loyaltyService.calculatePointsValue(customerId, points));
    }

    @GetMapping("/customer/{customerId}/can-redeem")
    @Operation(summary = "Check if points can be redeemed")
    public ResponseEntity<Boolean> canRedeemPoints(
            @PathVariable Long customerId,
            @RequestParam Integer points) {
        return ResponseEntity.ok(loyaltyService.canRedeemPoints(customerId, points));
    }
}