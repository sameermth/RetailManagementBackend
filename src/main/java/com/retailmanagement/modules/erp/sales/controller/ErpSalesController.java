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

    @GetMapping("/quotes")
    @Operation(summary = "List sales estimates and quotations")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<ErpSalesResponses.SalesQuoteSummaryResponse>> listQuotes(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.listQuotes(orgId).stream().map(this::toSalesQuoteSummary).toList());
    }

    @GetMapping("/quotes/{id}")
    @Operation(summary = "Get sales estimate or quotation by id")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<ErpSalesResponses.SalesQuoteResponse> getQuote(@PathVariable Long id) {
        return ErpApiResponse.ok(erpSalesService.getQuote(id));
    }

    @PostMapping("/quotes")
    @Operation(summary = "Create sales estimate or quotation")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesQuoteResponse> createQuote(@RequestBody @Valid ErpSalesDtos.CreateSalesQuoteRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.createQuote(orgId, branchId, request), "Sales quote created");
    }

    @PostMapping("/quotes/{id}/convert-to-order")
    @Operation(summary = "Convert estimate or quotation to sales order")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesOrderResponse> convertQuoteToOrder(@PathVariable Long id, @RequestBody(required = false) ErpSalesDtos.ConvertSalesQuoteRequest request) {
        ErpSalesDtos.ConvertSalesQuoteRequest safeRequest = request == null
                ? new ErpSalesDtos.ConvertSalesQuoteRequest(null, null, null, null, null)
                : request;
        return ErpApiResponse.ok(erpSalesService.convertQuoteToOrder(id, safeRequest), "Sales quote converted to order");
    }

    @PostMapping("/quotes/{id}/convert-to-invoice")
    @Operation(summary = "Convert estimate or quotation to sales invoice")
    @PreAuthorize("hasAnyAuthority('sales.create','sales.post')")
    public ErpApiResponse<ErpSalesResponses.SalesInvoiceResponse> convertQuoteToInvoice(@PathVariable Long id, @RequestBody(required = false) ErpSalesDtos.ConvertSalesQuoteRequest request) {
        ErpSalesDtos.ConvertSalesQuoteRequest safeRequest = request == null
                ? new ErpSalesDtos.ConvertSalesQuoteRequest(null, null, null, null, null)
                : request;
        return ErpApiResponse.ok(erpSalesService.convertQuoteToInvoice(id, safeRequest), "Sales quote converted to invoice");
    }

    @GetMapping("/orders")
    @Operation(summary = "List sales orders")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<ErpSalesResponses.SalesOrderSummaryResponse>> listOrders(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.listOrders(orgId).stream().map(this::toSalesOrderSummary).toList());
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get sales order by id")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<ErpSalesResponses.SalesOrderResponse> getOrder(@PathVariable Long id) {
        return ErpApiResponse.ok(erpSalesService.getOrder(id));
    }

    @PostMapping("/orders")
    @Operation(summary = "Create sales order")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesOrderResponse> createOrder(@RequestBody @Valid ErpSalesDtos.CreateSalesOrderRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.createOrder(orgId, branchId, request), "Sales order created");
    }

    @PostMapping("/orders/{id}/convert-to-invoice")
    @Operation(summary = "Convert sales order to sales invoice")
    @PreAuthorize("hasAnyAuthority('sales.create','sales.post')")
    public ErpApiResponse<ErpSalesResponses.SalesInvoiceResponse> convertOrderToInvoice(@PathVariable Long id, @RequestBody(required = false) ErpSalesDtos.ConvertSalesOrderRequest request) {
        ErpSalesDtos.ConvertSalesOrderRequest safeRequest = request == null
                ? new ErpSalesDtos.ConvertSalesOrderRequest(null, null, null, null, null)
                : request;
        return ErpApiResponse.ok(erpSalesService.convertOrderToInvoice(id, safeRequest), "Sales order converted to invoice");
    }

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

    private ErpSalesResponses.SalesQuoteSummaryResponse toSalesQuoteSummary(com.retailmanagement.modules.erp.sales.entity.SalesQuote quote) {
        return new ErpSalesResponses.SalesQuoteSummaryResponse(
                quote.getId(),
                quote.getOrganizationId(),
                quote.getBranchId(),
                quote.getWarehouseId(),
                quote.getCustomerId(),
                quote.getQuoteType(),
                quote.getQuoteNumber(),
                quote.getQuoteDate(),
                quote.getValidUntil(),
                quote.getTotalAmount(),
                quote.getConvertedSalesOrderId(),
                quote.getConvertedSalesInvoiceId(),
                quote.getStatus()
        );
    }

    private ErpSalesResponses.SalesOrderSummaryResponse toSalesOrderSummary(com.retailmanagement.modules.erp.sales.entity.SalesOrder order) {
        return new ErpSalesResponses.SalesOrderSummaryResponse(
                order.getId(),
                order.getOrganizationId(),
                order.getBranchId(),
                order.getWarehouseId(),
                order.getCustomerId(),
                order.getSourceQuoteId(),
                order.getOrderNumber(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getConvertedSalesInvoiceId(),
                order.getStatus()
        );
    }

    private ErpSalesResponses.CustomerReceiptResponse toCustomerReceiptResponse(CustomerReceipt receipt) {
        return new ErpSalesResponses.CustomerReceiptResponse(receipt.getId(), receipt.getOrganizationId(), receipt.getBranchId(),
                receipt.getCustomerId(), receipt.getReceiptNumber(), receipt.getReceiptDate(), receipt.getPaymentMethod(),
                receipt.getReferenceNumber(), receipt.getAmount(), receipt.getStatus(), receipt.getRemarks());
    }
}
