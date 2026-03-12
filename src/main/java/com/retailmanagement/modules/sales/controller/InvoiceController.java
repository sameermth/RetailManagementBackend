package com.retailmanagement.modules.sales.controller;

import com.retailmanagement.modules.sales.dto.response.InvoiceResponse;
import com.retailmanagement.modules.sales.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management endpoints")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/generate/{saleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CASHIER')")
    @Operation(summary = "Generate invoice for a sale")
    public ResponseEntity<InvoiceResponse> generateInvoice(@PathVariable Long saleId) {
        return new ResponseEntity<>(invoiceService.generateInvoice(saleId), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/number/{invoiceNumber}")
    @Operation(summary = "Get invoice by number")
    public ResponseEntity<InvoiceResponse> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.getInvoiceByNumber(invoiceNumber));
    }

    @GetMapping("/sale/{saleId}")
    @Operation(summary = "Get invoice by sale ID")
    public ResponseEntity<InvoiceResponse> getInvoiceBySaleId(@PathVariable Long saleId) {
        return ResponseEntity.ok(invoiceService.getInvoiceBySaleId(saleId));
    }

    @GetMapping
    @Operation(summary = "Get all invoices with pagination")
    public ResponseEntity<Page<InvoiceResponse>> getAllInvoices(
            @PageableDefault(size = 20, sort = "invoiceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getAllInvoices(pageable));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get invoices by date range")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(invoiceService.getInvoicesByDateRange(startDate, endDate));
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generate PDF for invoice")
    public ResponseEntity<byte[]> generateInvoicePdf(@PathVariable Long id) {
        byte[] pdfBytes = invoiceService.generateInvoicePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "invoice-" + id + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/email")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CASHIER')")
    @Operation(summary = "Send invoice by email")
    public ResponseEntity<Void> sendInvoiceByEmail(
            @PathVariable Long id,
            @RequestParam String email) {
        invoiceService.sendInvoiceByEmail(id, email);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/mark-printed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CASHIER')")
    @Operation(summary = "Mark invoice as printed")
    public ResponseEntity<Void> markAsPrinted(@PathVariable Long id) {
        invoiceService.markAsPrinted(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/mark-emailed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CASHIER')")
    @Operation(summary = "Mark invoice as emailed")
    public ResponseEntity<Void> markAsEmailed(@PathVariable Long id) {
        invoiceService.markAsEmailed(id);
        return ResponseEntity.ok().build();
    }
}