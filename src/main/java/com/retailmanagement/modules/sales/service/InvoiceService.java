package com.retailmanagement.modules.sales.service;

import com.retailmanagement.modules.sales.dto.response.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceService {

    InvoiceResponse generateInvoice(Long saleId);

    InvoiceResponse getInvoiceById(Long id);

    InvoiceResponse getInvoiceByNumber(String invoiceNumber);

    InvoiceResponse getInvoiceBySaleId(Long saleId);

    Page<InvoiceResponse> getAllInvoices(Pageable pageable);

    List<InvoiceResponse> getInvoicesByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    byte[] generateInvoicePdf(Long invoiceId);

    void sendInvoiceByEmail(Long invoiceId, String email);

    void markAsPrinted(Long invoiceId);

    void markAsEmailed(Long invoiceId);
}