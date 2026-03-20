package com.retailmanagement.modules.sales.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.common.utils.PdfGenerator;
import com.retailmanagement.modules.notification.dto.request.EmailRequest;
import com.retailmanagement.modules.sales.dto.response.InvoiceResponse;
import com.retailmanagement.modules.sales.mapper.InvoiceMapper;
import com.retailmanagement.modules.sales.model.Invoice;
import com.retailmanagement.modules.sales.model.Sale;
import com.retailmanagement.modules.sales.repository.InvoiceRepository;
import com.retailmanagement.modules.sales.repository.SaleRepository;
import com.retailmanagement.modules.sales.service.InvoiceService;
import com.retailmanagement.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SaleRepository saleRepository;
    private final InvoiceMapper invoiceMapper;
    private final PdfGenerator pdfGenerator;
    private final NotificationService notificationService;

    @Override
    public InvoiceResponse generateInvoice(Long saleId) {
        log.info("Generating invoice for sale ID: {}", saleId);

        // Check if invoice already exists
        invoiceRepository.findBySaleId(saleId).ifPresent(i -> {
            throw new BusinessException("Invoice already exists for this sale");
        });

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + saleId));

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber();

        // Create invoice
        Invoice invoice = invoiceMapper.toEntity(sale);
        invoice.setSale(sale);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setDueDate(sale.getDueDate());

        if (sale.getCustomer() != null) {
            invoice.setCustomerName(sale.getCustomer().getName());
            invoice.setCustomerAddress(sale.getCustomer().getAddress());
            invoice.setCustomerPhone(sale.getCustomer().getPhone());
            invoice.setCustomerEmail(sale.getCustomer().getEmail());
        }

        invoice.setSubtotal(sale.getSubtotal());
        invoice.setDiscountAmount(sale.getDiscountAmount());
        invoice.setTaxAmount(sale.getTaxAmount());
        invoice.setTotalAmount(sale.getTotalAmount());
        invoice.setPaidAmount(sale.getPaidAmount());
        invoice.setBalanceDue(sale.getPendingAmount());

        // Set default terms
        invoice.setTermsAndConditions(getDefaultTermsAndConditions());
        invoice.setBankDetails(getBankDetails());

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice generated successfully with number: {}", invoiceNumber);

        return invoiceMapper.toResponse(savedInvoice);
    }

    private String generateInvoiceNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String invoiceNumber = "INV-" + datePart + "-" + randomPart;

        while (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            invoiceNumber = "INV-" + datePart + "-" + randomPart;
        }

        return invoiceNumber;
    }

    private String getDefaultTermsAndConditions() {
        return "1. Goods once sold will not be taken back\n" +
                "2. Payment is due within 30 days\n" +
                "3. Interest will be charged on overdue payments\n" +
                "4. Subject to local jurisdiction";
    }

    private String getBankDetails() {
        return "Bank: Example Bank\n" +
                "Account Name: Retail Management\n" +
                "Account Number: 1234567890\n" +
                "IFSC Code: EXMP0001234\n" +
                "Branch: Main Branch";
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id) {
        log.debug("Fetching invoice with ID: {}", id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        log.debug("Fetching invoice with number: {}", invoiceNumber);

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with number: " + invoiceNumber));

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceBySaleId(Long saleId) {
        log.debug("Fetching invoice for sale ID: {}", saleId);

        Invoice invoice = invoiceRepository.findBySaleId(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for sale id: " + saleId));

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> getAllInvoices(Pageable pageable) {
        log.debug("Fetching all invoices with pagination");

        return invoiceRepository.findAll(pageable)
                .map(invoiceMapper::toResponse);
    }

    @Override
    public List<InvoiceResponse> getInvoicesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching invoices between {} and {}", startDate, endDate);

        return invoiceRepository.findByInvoiceDateBetween(startDate, endDate).stream()
                .map(invoiceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] generateInvoicePdf(Long invoiceId) {
        log.info("Generating PDF for invoice ID: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        InvoiceResponse invoiceResponse = invoiceMapper.toResponse(invoice);

        // Generate PDF using PdfGenerator
        byte[] pdfBytes = pdfGenerator.generateInvoicePdf(invoiceResponse);

        // Update PDF URL
        String pdfUrl = "/invoices/pdf/" + invoice.getInvoiceNumber() + ".pdf";
        invoice.setPdfUrl(pdfUrl);
        invoiceRepository.save(invoice);

        return pdfBytes;
    }

    @Override
    public void sendInvoiceByEmail(Long invoiceId, String email) {
        log.info("Sending invoice ID: {} to email: {}", invoiceId, email);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        // Generate PDF if not exists
        if (invoice.getPdfUrl() == null) {
            generateInvoicePdf(invoiceId);
        }

        // Prepare email content
        String subject = "Invoice " + invoice.getInvoiceNumber();
        String body = "Dear " + invoice.getCustomerName() + ",\n\n" +
                "Please find attached your invoice " + invoice.getInvoiceNumber() + ".\n\n" +
                "Total Amount: ₹" + invoice.getTotalAmount() + "\n" +
                "Due Date: " + invoice.getDueDate() + "\n\n" +
                "Thank you for your business!\n\n" +
                "Regards,\nRetail Management Team";
        List<EmailRequest.Attachment> attachments = new ArrayList<>();
        attachments.add(new EmailRequest.Attachment() {{
            setFileName(invoice.getInvoiceNumber() + ".pdf");
            setContent(pdfGenerator.generateInvoicePdf(invoiceMapper.toResponse(invoice)));
            setContentType("application/pdf");
        }});

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(email);
        emailRequest.setSubject(subject);
        emailRequest.setContent(body);
        emailRequest.setAttachments(attachments);

        // Send email with attachment
        notificationService.sendEmail(emailRequest);

        invoice.setIsEmailed(true);
        invoiceRepository.save(invoice);
    }

    @Override
    public void markAsPrinted(Long invoiceId) {
        log.info("Marking invoice ID: {} as printed", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        invoice.setIsPrinted(true);
        invoiceRepository.save(invoice);
    }

    @Override
    public void markAsEmailed(Long invoiceId) {
        log.info("Marking invoice ID: {} as emailed", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        invoice.setIsEmailed(true);
        invoiceRepository.save(invoice);
    }
}