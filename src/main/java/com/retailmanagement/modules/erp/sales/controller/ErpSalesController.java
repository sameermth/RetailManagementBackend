package com.retailmanagement.modules.erp.sales.controller;

import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.document.dto.ErpDocumentDtos;
import com.retailmanagement.modules.erp.document.service.ErpDocumentService;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesDtos;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesResponses;
import com.retailmanagement.modules.erp.sales.entity.CustomerReceipt;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesDispatch;
import com.retailmanagement.modules.erp.sales.service.ErpSalesService;
import com.retailmanagement.modules.erp.sales.service.SalesDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/sales")
@RequiredArgsConstructor
@Tag(name = "ERP Sales", description = "ERP sales invoices and customer receipt endpoints")
public class ErpSalesController {

    private final ErpSalesService erpSalesService;
    private final SalesDispatchService salesDispatchService;
    private final ErpDocumentService erpDocumentService;

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

    @GetMapping("/quotes/{id}/pdf")
    @Operation(summary = "Download sales estimate or quotation PDF")
    @PreAuthorize("hasAuthority('sales.view')")
    public ResponseEntity<ByteArrayResource> downloadQuotePdf(@PathVariable Long id) {
        ErpSalesResponses.SalesQuoteResponse quote = erpSalesService.getQuote(id);
        return pdfResponse(erpDocumentService.generateSalesQuotePdf(id), quote.quoteNumber() + ".pdf");
    }

    @PostMapping("/quotes/{id}/send")
    @Operation(summary = "Email sales estimate or quotation PDF")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<Void> sendQuotePdf(@PathVariable Long id, @RequestBody(required = false) ErpDocumentDtos.SendDocumentRequest request) {
        erpDocumentService.sendSalesQuote(id, request);
        return ErpApiResponse.ok(null, "Sales quote emailed");
    }

    @PostMapping("/quotes")
    @Operation(summary = "Create sales estimate or quotation")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesQuoteResponse> createQuote(@RequestBody @Valid ErpSalesDtos.CreateSalesQuoteRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.createQuote(orgId, branchId, request), "Sales quote created");
    }

    @PostMapping("/quotes/{id}/cancel")
    @Operation(summary = "Cancel sales estimate or quotation")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesQuoteResponse> cancelQuote(@PathVariable Long id,
                                                                            @RequestBody @Valid ErpSalesDtos.CancelSalesDocumentRequest request) {
        return ErpApiResponse.ok(erpSalesService.cancelQuote(id, request), "Sales quote cancelled");
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

    @GetMapping("/orders/{id}/pdf")
    @Operation(summary = "Download sales order PDF")
    @PreAuthorize("hasAuthority('sales.view')")
    public ResponseEntity<ByteArrayResource> downloadOrderPdf(@PathVariable Long id) {
        ErpSalesResponses.SalesOrderResponse order = erpSalesService.getOrder(id);
        return pdfResponse(erpDocumentService.generateSalesOrderPdf(id), order.orderNumber() + ".pdf");
    }

    @PostMapping("/orders/{id}/send")
    @Operation(summary = "Email sales order PDF")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<Void> sendOrderPdf(@PathVariable Long id, @RequestBody(required = false) ErpDocumentDtos.SendDocumentRequest request) {
        erpDocumentService.sendSalesOrder(id, request);
        return ErpApiResponse.ok(null, "Sales order emailed");
    }

    @PostMapping("/orders")
    @Operation(summary = "Create sales order")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesOrderResponse> createOrder(@RequestBody @Valid ErpSalesDtos.CreateSalesOrderRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.createOrder(orgId, branchId, request), "Sales order created");
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel sales order")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesOrderResponse> cancelOrder(@PathVariable Long id,
                                                                            @RequestBody @Valid ErpSalesDtos.CancelSalesDocumentRequest request) {
        return ErpApiResponse.ok(erpSalesService.cancelOrder(id, request), "Sales order cancelled");
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

    @GetMapping("/invoices/{id}/pdf")
    @Operation(summary = "Download sales invoice PDF")
    @PreAuthorize("hasAuthority('sales.view')")
    public ResponseEntity<ByteArrayResource> downloadInvoicePdf(@PathVariable Long id) {
        ErpSalesResponses.SalesInvoiceResponse invoice = erpSalesService.getInvoice(id);
        return pdfResponse(erpDocumentService.generateSalesInvoicePdf(id), invoice.invoiceNumber() + ".pdf");
    }

    @PostMapping("/invoices/{id}/send")
    @Operation(summary = "Email sales invoice PDF")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<Void> sendInvoicePdf(@PathVariable Long id, @RequestBody(required = false) ErpDocumentDtos.SendDocumentRequest request) {
        erpDocumentService.sendSalesInvoice(id, request);
        return ErpApiResponse.ok(null, "Sales invoice emailed");
    }

    @PostMapping("/invoices")
    @Operation(summary = "Create sales invoice")
    @PreAuthorize("hasAnyAuthority('sales.create','sales.post')")
    public ErpApiResponse<ErpSalesResponses.SalesInvoiceResponse> createInvoice(@RequestBody @Valid ErpSalesDtos.CreateSalesInvoiceRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.createInvoice(orgId, branchId, request), "Sales invoice created");
    }

    @GetMapping("/payment-requests")
    @Operation(summary = "List invoice payment requests")
    @PreAuthorize("hasAnyAuthority('sales.view','payment.receive')")
    public ErpApiResponse<List<ErpSalesResponses.SalesInvoicePaymentRequestResponse>> listPaymentRequests(
            @RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(
                erpSalesService.listPaymentRequests(orgId).stream().map(erpSalesService::toPaymentRequestResponse).toList()
        );
    }

    @GetMapping("/payment-gateway/providers")
    @Operation(summary = "List available payment gateway providers")
    @PreAuthorize("hasAnyAuthority('sales.view','payment.receive')")
    public ErpApiResponse<List<ErpSalesResponses.PaymentGatewayProviderResponse>> listPaymentGatewayProviders() {
        return ErpApiResponse.ok(erpSalesService.listPaymentGatewayProviders());
    }

    @GetMapping("/payment-requests/{id}")
    @Operation(summary = "Get invoice payment request")
    @PreAuthorize("hasAnyAuthority('sales.view','payment.receive')")
    public ErpApiResponse<ErpSalesResponses.SalesInvoicePaymentRequestResponse> getPaymentRequest(@PathVariable Long id) {
        return ErpApiResponse.ok(erpSalesService.toPaymentRequestResponse(erpSalesService.getPaymentRequest(id)));
    }

    @GetMapping("/invoices/{id}/payment-requests")
    @Operation(summary = "List payment requests for invoice")
    @PreAuthorize("hasAnyAuthority('sales.view','payment.receive')")
    public ErpApiResponse<List<ErpSalesResponses.SalesInvoicePaymentRequestResponse>> listInvoicePaymentRequests(@PathVariable Long id) {
        return ErpApiResponse.ok(
                erpSalesService.listInvoicePaymentRequests(id).stream().map(erpSalesService::toPaymentRequestResponse).toList()
        );
    }

    @PostMapping("/invoices/{id}/payment-requests")
    @Operation(summary = "Create payment request for invoice")
    @PreAuthorize("hasAuthority('payment.receive')")
    public ErpApiResponse<ErpSalesResponses.SalesInvoicePaymentRequestResponse> createPaymentRequest(
            @PathVariable Long id,
            @RequestBody @Valid ErpSalesDtos.CreateSalesInvoicePaymentRequestRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(
                erpSalesService.toPaymentRequestResponse(erpSalesService.createPaymentRequest(orgId, branchId, id, request)),
                "Invoice payment request created"
        );
    }

    @PostMapping("/payment-requests/{id}/cancel")
    @Operation(summary = "Cancel invoice payment request")
    @PreAuthorize("hasAuthority('payment.receive')")
    public ErpApiResponse<ErpSalesResponses.SalesInvoicePaymentRequestResponse> cancelPaymentRequest(
            @PathVariable Long id,
            @RequestBody @Valid ErpSalesDtos.CancelSalesInvoicePaymentRequest request) {
        return ErpApiResponse.ok(
                erpSalesService.toPaymentRequestResponse(erpSalesService.cancelPaymentRequest(id, request)),
                "Invoice payment request cancelled"
        );
    }

    @PostMapping("/payment-requests/{id}/sync-provider-status")
    @Operation(summary = "Sync invoice payment request status with configured gateway provider")
    @PreAuthorize("hasAuthority('payment.receive')")
    public ErpApiResponse<ErpSalesResponses.SalesInvoicePaymentRequestResponse> syncPaymentRequestProviderStatus(@PathVariable Long id) {
        return ErpApiResponse.ok(
                erpSalesService.toPaymentRequestResponse(erpSalesService.syncPaymentRequestProviderStatus(id)),
                "Invoice payment request provider status synchronized"
        );
    }

    @GetMapping("/dispatches")
    @Operation(summary = "List sales dispatch documents")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<ErpSalesResponses.SalesDispatchSummaryResponse>> listDispatches(@RequestParam(required = false) Long organizationId,
                                                                                               @RequestParam(required = false) Long salesInvoiceId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(
                salesDispatchService.listDispatches(orgId, salesInvoiceId).stream().map(this::toSalesDispatchSummary).toList()
        );
    }

    @GetMapping("/dispatches/{id}")
    @Operation(summary = "Get sales dispatch by id")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<ErpSalesResponses.SalesDispatchResponse> getDispatch(@PathVariable Long id) {
        return ErpApiResponse.ok(salesDispatchService.getDispatch(id));
    }

    @GetMapping("/dispatches/{id}/pdf")
    @Operation(summary = "Download sales dispatch PDF")
    @PreAuthorize("hasAuthority('sales.view')")
    public ResponseEntity<ByteArrayResource> downloadDispatchPdf(@PathVariable Long id) {
        ErpSalesResponses.SalesDispatchResponse dispatch = salesDispatchService.getDispatch(id);
        return pdfResponse(erpDocumentService.generateSalesDispatchPdf(id), dispatch.dispatchNumber() + ".pdf");
    }

    @PostMapping("/dispatches/{id}/send")
    @Operation(summary = "Email sales dispatch PDF")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<Void> sendDispatchPdf(@PathVariable Long id, @RequestBody(required = false) ErpDocumentDtos.SendDocumentRequest request) {
        erpDocumentService.sendSalesDispatch(id, request);
        return ErpApiResponse.ok(null, "Sales dispatch emailed");
    }

    @PostMapping("/invoices/{id}/dispatches")
    @Operation(summary = "Create sales dispatch for invoice")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesDispatchResponse> createDispatch(@PathVariable Long id,
                                                                                  @RequestBody @Valid ErpSalesDtos.CreateSalesDispatchRequest request) {
        Long orgId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);
        return ErpApiResponse.ok(salesDispatchService.createDispatch(id, orgId, branchId, request), "Sales dispatch created");
    }

    @PostMapping("/dispatches/{id}/status")
    @Operation(summary = "Update sales dispatch status")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesDispatchResponse> updateDispatchStatus(@PathVariable Long id,
                                                                                         @RequestBody @Valid ErpSalesDtos.UpdateSalesDispatchStatusRequest request) {
        return ErpApiResponse.ok(salesDispatchService.updateDispatchStatus(id, request), "Sales dispatch status updated");
    }

    @PostMapping("/dispatches/{id}/pick")
    @Operation(summary = "Pick sales dispatch lines")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesDispatchResponse> pickDispatch(@PathVariable Long id,
                                                                                 @RequestBody @Valid ErpSalesDtos.PickSalesDispatchRequest request) {
        return ErpApiResponse.ok(salesDispatchService.pickDispatch(id, request), "Sales dispatch picked");
    }

    @PostMapping("/dispatches/{id}/pack")
    @Operation(summary = "Pack sales dispatch lines")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpSalesResponses.SalesDispatchResponse> packDispatch(@PathVariable Long id,
                                                                                 @RequestBody @Valid ErpSalesDtos.PackSalesDispatchRequest request) {
        return ErpApiResponse.ok(salesDispatchService.packDispatch(id, request), "Sales dispatch packed");
    }

    @GetMapping("/receipts")
    @Operation(summary = "List customer receipts")
    @PreAuthorize("hasAuthority('sales.view')")
    public ErpApiResponse<List<ErpSalesResponses.CustomerReceiptResponse>> listReceipts(@RequestParam(required = false) Long organizationId) {
        Long orgId = organizationId != null ? organizationId : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        return ErpApiResponse.ok(erpSalesService.listReceipts(orgId).stream().map(this::toCustomerReceiptResponse).toList());
    }

    @GetMapping("/receipts/{id}/pdf")
    @Operation(summary = "Download customer receipt PDF")
    @PreAuthorize("hasAnyAuthority('sales.view','payment.receive')")
    public ResponseEntity<ByteArrayResource> downloadReceiptPdf(@PathVariable Long id) {
        CustomerReceipt receipt = erpSalesService.getReceipt(id);
        return pdfResponse(erpDocumentService.generateCustomerReceiptPdf(id), receipt.getReceiptNumber() + ".pdf");
    }

    @PostMapping("/receipts/{id}/send")
    @Operation(summary = "Email customer receipt PDF")
    @PreAuthorize("hasAnyAuthority('sales.view','payment.receive')")
    public ErpApiResponse<Void> sendReceiptPdf(@PathVariable Long id, @RequestBody(required = false) ErpDocumentDtos.SendDocumentRequest request) {
        erpDocumentService.sendCustomerReceipt(id, request);
        return ErpApiResponse.ok(null, "Customer receipt emailed");
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
                null, null, invoice.getStatus(), salesDispatchService.buildInvoiceDispatchSummary(invoice.getId()),
                erpSalesService.buildInvoicePaymentSummary(invoice.getId()));
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
                order.getExpectedFulfillmentBy(),
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

    private ErpSalesResponses.SalesDispatchSummaryResponse toSalesDispatchSummary(SalesDispatch dispatch) {
        ErpSalesResponses.SalesDispatchResponse response = salesDispatchService.getDispatch(dispatch.getId());
        return new ErpSalesResponses.SalesDispatchSummaryResponse(
                response.id(),
                response.organizationId(),
                response.branchId(),
                response.salesInvoiceId(),
                response.invoiceNumber(),
                response.dispatchNumber(),
                response.dispatchDate(),
                response.expectedDeliveryDate(),
                response.status(),
                response.transporterName(),
                response.trackingNumber(),
                response.dispatchedAt(),
                response.deliveredAt()
        );
    }

    private ResponseEntity<ByteArrayResource> pdfResponse(byte[] pdf, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(new ByteArrayResource(pdf));
    }
}
