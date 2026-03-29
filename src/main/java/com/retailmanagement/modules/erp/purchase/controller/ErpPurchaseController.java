package com.retailmanagement.modules.erp.purchase.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.purchase.dto.ErpPurchaseDtos;
import com.retailmanagement.modules.erp.purchase.dto.ErpPurchaseResponses;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrder;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceipt;
import com.retailmanagement.modules.erp.purchase.entity.SupplierPayment;
import com.retailmanagement.modules.erp.purchase.service.ErpPurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/purchases")
@RequiredArgsConstructor
@Tag(name = "ERP Purchases", description = "ERP purchase orders, receipts, and supplier payment endpoints")
public class ErpPurchaseController {

    private final ErpPurchaseService erpPurchaseService;

    @GetMapping("/orders")
    @Operation(summary = "List purchase orders")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<List<ErpPurchaseResponses.PurchaseOrderSummaryResponse>> listPurchaseOrders(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpPurchaseService.listPurchaseOrders(orgId).stream().map(this::toPurchaseOrderSummary).toList());
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get purchase order by id")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<ErpPurchaseResponses.PurchaseOrderResponse> getPurchaseOrder(@PathVariable Long id) {
        return ErpApiResponse.ok(erpPurchaseService.getPurchaseOrder(id));
    }

    @PostMapping("/orders")
    @Operation(summary = "Create purchase order")
    @PreAuthorize("hasAuthority('purchase.create')")
    public ErpApiResponse<ErpPurchaseResponses.PurchaseOrderResponse> createPurchaseOrder(@RequestBody @Valid ErpPurchaseDtos.CreatePurchaseOrderRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpPurchaseService.createPurchaseOrder(orgId, branchId, request), "Purchase order created");
    }

    @GetMapping("/receipts")
    @Operation(summary = "List purchase receipts")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<List<ErpPurchaseResponses.PurchaseReceiptSummaryResponse>> listPurchaseReceipts(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpPurchaseService.listPurchaseReceipts(orgId).stream().map(this::toPurchaseReceiptSummary).toList());
    }

    @GetMapping("/receipts/{id}")
    @Operation(summary = "Get purchase receipt by id")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ErpApiResponse<ErpPurchaseResponses.PurchaseReceiptResponse> getPurchaseReceipt(@PathVariable Long id) {
        return ErpApiResponse.ok(erpPurchaseService.getPurchaseReceipt(id));
    }

    @PostMapping("/receipts")
    @Operation(summary = "Create purchase receipt")
    @PreAuthorize("hasAnyAuthority('purchase.post','inventory.receive')")
    public ErpApiResponse<ErpPurchaseResponses.PurchaseReceiptResponse> createPurchaseReceipt(@RequestBody @Valid ErpPurchaseDtos.CreatePurchaseReceiptRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpPurchaseService.createPurchaseReceipt(orgId, branchId, request), "Purchase receipt created");
    }

    @GetMapping("/supplier-payments")
    @Operation(summary = "List supplier payments")
    @PreAuthorize("hasAnyAuthority('purchase.view','payment.pay')")
    public ErpApiResponse<List<ErpPurchaseResponses.SupplierPaymentResponse>> listSupplierPayments(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpPurchaseService.listSupplierPayments(orgId).stream().map(this::toSupplierPaymentResponse).toList());
    }

    @PostMapping("/supplier-payments")
    @Operation(summary = "Create supplier payment")
    @PreAuthorize("hasAuthority('payment.pay')")
    public ErpApiResponse<ErpPurchaseResponses.SupplierPaymentResponse> createSupplierPayment(@RequestBody @Valid ErpPurchaseDtos.CreateSupplierPaymentRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(toSupplierPaymentResponse(erpPurchaseService.createSupplierPayment(orgId, branchId, request)), "Supplier payment created");
    }

    @PostMapping("/supplier-payments/{id}/allocate")
    @Operation(summary = "Allocate supplier payment")
    @PreAuthorize("hasAuthority('payment.pay')")
    public ErpApiResponse<ErpPurchaseResponses.SupplierPaymentResponse> allocateSupplierPayment(@PathVariable Long id,
                                                                                                @RequestBody @Valid ErpPurchaseDtos.AllocateSupplierPaymentRequest request) {
        return ErpApiResponse.ok(toSupplierPaymentResponse(erpPurchaseService.allocateSupplierPayment(id, request)), "Supplier payment allocated");
    }

    private ErpPurchaseResponses.PurchaseOrderSummaryResponse toPurchaseOrderSummary(PurchaseOrder order) {
        return new ErpPurchaseResponses.PurchaseOrderSummaryResponse(order.getId(), order.getOrganizationId(), order.getBranchId(),
                order.getSupplierId(), order.getPoNumber(), order.getPoDate(), order.getSellerGstin(), order.getSupplierGstin(),
                order.getPlaceOfSupplyStateCode(), order.getSubtotal(), order.getTaxAmount(), order.getTotalAmount(), order.getStatus());
    }

    private ErpPurchaseResponses.PurchaseReceiptSummaryResponse toPurchaseReceiptSummary(PurchaseReceipt receipt) {
        return new ErpPurchaseResponses.PurchaseReceiptSummaryResponse(receipt.getId(), receipt.getOrganizationId(), receipt.getBranchId(),
                receipt.getWarehouseId(), receipt.getSupplierId(), receipt.getReceiptNumber(), receipt.getReceiptDate(), receipt.getDueDate(),
                receipt.getSellerGstin(), receipt.getSupplierGstin(), receipt.getPlaceOfSupplyStateCode(), receipt.getSubtotal(),
                receipt.getTaxAmount(), receipt.getTotalAmount(), null, null, receipt.getStatus());
    }

    private ErpPurchaseResponses.SupplierPaymentResponse toSupplierPaymentResponse(SupplierPayment payment) {
        return new ErpPurchaseResponses.SupplierPaymentResponse(payment.getId(), payment.getOrganizationId(), payment.getBranchId(),
                payment.getSupplierId(), payment.getPaymentNumber(), payment.getPaymentDate(), payment.getPaymentMethod(),
                payment.getReferenceNumber(), payment.getAmount(), payment.getStatus(), payment.getRemarks());
    }
}
