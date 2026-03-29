package com.retailmanagement.modules.erp.sales.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesDtos;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesResponses;
import com.retailmanagement.modules.erp.sales.entity.CustomerReceipt;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.service.ErpSalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/sales")
@RequiredArgsConstructor
@Tag(name = "ERP Sales", description = "ERP sales invoices and customer receipt endpoints")
public class ErpSalesController {

    private final ErpSalesService erpSalesService;

    @GetMapping("/invoices")
    @Operation(summary = "List sales invoices")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<ErpSalesResponses.SalesInvoiceSummaryResponse>> listInvoices(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.listInvoices(orgId).stream().map(this::toSalesInvoiceSummary).toList());
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "Get sales invoice by id")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<ErpSalesResponses.SalesInvoiceResponse> getInvoice(@PathVariable Long id) {
        return ErpApiResponse.ok(erpSalesService.getInvoice(id));
    }

    @PostMapping("/invoices")
    @Operation(summary = "Create sales invoice")
    @PreAuthorize("hasAnyAuthority('sales.create','sales.post')")
    public ErpApiResponse<ErpSalesResponses.SalesInvoiceResponse> createInvoice(@RequestBody @Valid ErpSalesDtos.CreateSalesInvoiceRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.createInvoice(orgId, branchId, request), "Sales invoice created");
    }

    @GetMapping("/receipts")
    @Operation(summary = "List customer receipts")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<ErpSalesResponses.CustomerReceiptResponse>> listReceipts(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.listReceipts(orgId).stream().map(this::toCustomerReceiptResponse).toList());
    }

    @PostMapping("/receipts")
    @Operation(summary = "Create customer receipt")
    @PreAuthorize("hasAuthority('payment.receive')")
    public ErpApiResponse<ErpSalesResponses.CustomerReceiptResponse> createReceipt(@RequestBody @Valid ErpSalesDtos.CreateCustomerReceiptRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(toCustomerReceiptResponse(erpSalesService.createReceipt(orgId, branchId, request)), "Customer receipt created");
    }

    @PostMapping("/receipts/{id}/allocate")
    @Operation(summary = "Allocate customer receipt")
    @PreAuthorize("hasAuthority('payment.receive')")
    public ErpApiResponse<ErpSalesResponses.CustomerReceiptResponse> allocateReceipt(@PathVariable Long id, @RequestBody @Valid ErpSalesDtos.AllocateReceiptRequest request) {
        return ErpApiResponse.ok(toCustomerReceiptResponse(erpSalesService.allocateReceipt(id, request)), "Customer receipt allocated");
    }

    private ErpSalesResponses.SalesInvoiceSummaryResponse toSalesInvoiceSummary(SalesInvoice invoice) {
        return new ErpSalesResponses.SalesInvoiceSummaryResponse(invoice.getId(), invoice.getOrganizationId(), invoice.getBranchId(),
                invoice.getWarehouseId(), invoice.getCustomerId(), invoice.getInvoiceNumber(), invoice.getInvoiceDate(),
                invoice.getDueDate(), invoice.getSellerGstin(), invoice.getCustomerGstin(), invoice.getPlaceOfSupplyStateCode(),
                invoice.getSubtotal(), invoice.getDiscountAmount(), invoice.getTaxAmount(), invoice.getTotalAmount(),
                null, null, invoice.getStatus());
    }

    private ErpSalesResponses.CustomerReceiptResponse toCustomerReceiptResponse(CustomerReceipt receipt) {
        return new ErpSalesResponses.CustomerReceiptResponse(receipt.getId(), receipt.getOrganizationId(), receipt.getBranchId(),
                receipt.getCustomerId(), receipt.getReceiptNumber(), receipt.getReceiptDate(), receipt.getPaymentMethod(),
                receipt.getReferenceNumber(), receipt.getAmount(), receipt.getStatus(), receipt.getRemarks());
    }
}
