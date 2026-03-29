package com.retailmanagement.modules.erp.subscription.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.subscription.dto.SubscriptionDtos;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/subscriptions")
@RequiredArgsConstructor
@Tag(name = "ERP Subscriptions", description = "ERP subscription and plan activation endpoints")
public class SubscriptionController {

    private final SubscriptionManagementService subscriptionManagementService;

    @GetMapping("/current")
    @Operation(summary = "Get current organization subscription")
    @PreAuthorize("hasAuthority('subscription.view')")
    public ErpApiResponse<SubscriptionDtos.OrganizationSubscriptionResponse> current(@RequestParam Long organizationId) {
        return ErpApiResponse.ok(subscriptionManagementService.currentSubscription(organizationId));
    }

    @PostMapping("/organizations/{organizationId}/activate")
    @Operation(summary = "Activate organization subscription")
    @PreAuthorize("hasAuthority('subscription.manage')")
    public ErpApiResponse<SubscriptionDtos.OrganizationSubscriptionResponse> activate(
            @PathVariable Long organizationId,
            @Valid @RequestBody SubscriptionDtos.ActivateOrganizationSubscriptionRequest request
    ) {
        return ErpApiResponse.ok(
                subscriptionManagementService.activateSubscription(organizationId, request),
                "Organization subscription updated. Existing logins must refresh."
        );
    }
}
