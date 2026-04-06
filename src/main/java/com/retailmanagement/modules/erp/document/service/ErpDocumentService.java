package com.retailmanagement.modules.erp.document.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.entity.Uom;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.UomRepository;
import com.retailmanagement.modules.erp.document.dto.ErpDocumentDtos;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.entity.Warehouse;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.foundation.repository.WarehouseRepository;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.entity.Supplier;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import com.retailmanagement.modules.erp.purchase.dto.ErpPurchaseResponses;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrder;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceipt;
import com.retailmanagement.modules.erp.purchase.entity.SupplierPayment;
import com.retailmanagement.modules.erp.purchase.entity.SupplierPaymentAllocation;
import com.retailmanagement.modules.erp.purchase.repository.SupplierPaymentAllocationRepository;
import com.retailmanagement.modules.erp.purchase.repository.SupplierPaymentRepository;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseOrderRepository;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseReceiptRepository;
import com.retailmanagement.modules.erp.purchase.service.ErpPurchaseService;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesResponses;
import com.retailmanagement.modules.erp.sales.entity.CustomerReceipt;
import com.retailmanagement.modules.erp.sales.entity.CustomerReceiptAllocation;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptAllocationRepository;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.sales.service.ErpSalesService;
import com.retailmanagement.modules.notification.dto.request.EmailRequest;
import com.retailmanagement.modules.notification.service.EmailService;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErpDocumentService {

    private final ErpSalesService erpSalesService;
    private final ErpPurchaseService erpPurchaseService;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final StoreProductRepository storeProductRepository;
    private final UomRepository uomRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final CustomerReceiptRepository customerReceiptRepository;
    private final CustomerReceiptAllocationRepository customerReceiptAllocationRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierPaymentAllocationRepository supplierPaymentAllocationRepository;
    private final EmailService emailService;

    public byte[] generateSalesQuotePdf(Long id) {
        ErpSalesResponses.SalesQuoteResponse quote = erpSalesService.getQuote(id);
        return generateSalesDocumentPdf(
                "Estimate / Quotation",
                quote.quoteNumber(),
                quote.quoteDate(),
                quote.validUntil(),
                quote.organizationId(),
                quote.branchId(),
                quote.warehouseId(),
                quote.customerId(),
                quote.sellerGstin(),
                quote.customerGstin(),
                quote.placeOfSupplyStateCode(),
                quote.subtotal(),
                quote.discountAmount(),
                quote.taxAmount(),
                quote.totalAmount(),
                quote.remarks(),
                enrichSalesLines(quote.organizationId(), quote.lines()),
                summarizeSalesDocumentTaxes(quote.lines())
        );
    }

    public byte[] generateSalesOrderPdf(Long id) {
        ErpSalesResponses.SalesOrderResponse order = erpSalesService.getOrder(id);
        return generateSalesDocumentPdf(
                "Sales Order",
                order.orderNumber(),
                order.orderDate(),
                null,
                order.organizationId(),
                order.branchId(),
                order.warehouseId(),
                order.customerId(),
                order.sellerGstin(),
                order.customerGstin(),
                order.placeOfSupplyStateCode(),
                order.subtotal(),
                order.discountAmount(),
                order.taxAmount(),
                order.totalAmount(),
                order.remarks(),
                enrichSalesLines(order.organizationId(), order.lines()),
                summarizeSalesDocumentTaxes(order.lines())
        );
    }

    public byte[] generateSalesInvoicePdf(Long id) {
        ErpSalesResponses.SalesInvoiceResponse invoice = erpSalesService.getInvoice(id);
        SalesInvoice invoiceEntity = salesInvoiceRepository.findById(invoice.id())
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + id));
        return generateSalesDocumentPdf(
                "Tax Invoice",
                invoice.invoiceNumber(),
                invoice.invoiceDate(),
                invoice.dueDate(),
                invoice.organizationId(),
                invoice.branchId(),
                invoice.warehouseId(),
                invoice.customerId(),
                invoice.sellerGstin(),
                invoice.customerGstin(),
                invoice.placeOfSupplyStateCode(),
                invoice.subtotal(),
                invoice.discountAmount(),
                invoice.taxAmount(),
                invoice.totalAmount(),
                invoiceEntity.getRemarks(),
                enrichSalesInvoiceLines(invoice.organizationId(), invoice.lines()),
                summarizeSalesInvoiceTaxes(invoice.lines())
        );
    }

    public byte[] generatePurchaseOrderPdf(Long id) {
        ErpPurchaseResponses.PurchaseOrderResponse order = erpPurchaseService.getPurchaseOrder(id);
        PurchaseOrder entity = purchaseOrderRepository.findById(order.id())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));
        return generatePurchaseDocumentPdf(
                "Purchase Order",
                order.poNumber(),
                order.poDate(),
                null,
                order.organizationId(),
                order.branchId(),
                null,
                order.supplierId(),
                order.sellerGstin(),
                order.supplierGstin(),
                order.placeOfSupplyStateCode(),
                order.subtotal(),
                BigDecimal.ZERO,
                order.taxAmount(),
                order.totalAmount(),
                entity.getRemarks(),
                enrichPurchaseLines(order.lines()),
                summarizePurchaseTaxes(order.lines())
        );
    }

    public byte[] generatePurchaseReceiptPdf(Long id) {
        ErpPurchaseResponses.PurchaseReceiptResponse receipt = erpPurchaseService.getPurchaseReceipt(id);
        PurchaseReceipt entity = purchaseReceiptRepository.findById(receipt.id())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase receipt not found: " + id));
        return generatePurchaseDocumentPdf(
                "Purchase Receipt",
                receipt.receiptNumber(),
                receipt.receiptDate(),
                receipt.dueDate(),
                receipt.organizationId(),
                receipt.branchId(),
                receipt.warehouseId(),
                receipt.supplierId(),
                receipt.sellerGstin(),
                receipt.supplierGstin(),
                receipt.placeOfSupplyStateCode(),
                receipt.subtotal(),
                BigDecimal.ZERO,
                receipt.taxAmount(),
                receipt.totalAmount(),
                entity.getRemarks(),
                enrichPurchaseLines(receipt.lines()),
                summarizePurchaseTaxes(receipt.lines())
        );
    }

    public byte[] generateCustomerReceiptPdf(Long id) {
        CustomerReceipt receipt = customerReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer receipt not found: " + id));
        Organization organization = requireOrganization(receipt.getOrganizationId());
        Branch branch = requireBranch(receipt.getOrganizationId(), receipt.getBranchId());
        Customer customer = requireCustomer(receipt.getOrganizationId(), receipt.getCustomerId());
        List<PaymentAllocationLine> allocations = enrichCustomerReceiptAllocations(
                customerReceiptAllocationRepository.findByCustomerReceiptIdOrderByIdAsc(receipt.getId())
        );
        return renderPaymentPdf(
                "Customer Receipt",
                receipt.getReceiptNumber(),
                receipt.getReceiptDate(),
                organization,
                branch,
                firstNonBlank(customer.getFullName(), customer.getTradeName(), customer.getLegalName()),
                customer.getLegalName(),
                customer.getPhone(),
                customer.getEmail(),
                firstNonBlank(customer.getBillingAddress(), customer.getShippingAddress()),
                customer.getGstin(),
                receipt.getPaymentMethod(),
                receipt.getReferenceNumber(),
                receipt.getAmount(),
                receipt.getRemarks(),
                allocations
        );
    }

    public byte[] generateSupplierPaymentPdf(Long id) {
        SupplierPayment payment = supplierPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier payment not found: " + id));
        Organization organization = requireOrganization(payment.getOrganizationId());
        Branch branch = requireBranch(payment.getOrganizationId(), payment.getBranchId());
        Supplier supplier = requireSupplier(payment.getOrganizationId(), payment.getSupplierId());
        List<PaymentAllocationLine> allocations = enrichSupplierPaymentAllocations(
                supplierPaymentAllocationRepository.findBySupplierPaymentIdOrderByIdAsc(payment.getId())
        );
        return renderPaymentPdf(
                "Supplier Payment Voucher",
                payment.getPaymentNumber(),
                payment.getPaymentDate(),
                organization,
                branch,
                firstNonBlank(supplier.getTradeName(), supplier.getName(), supplier.getLegalName()),
                supplier.getLegalName(),
                supplier.getPhone(),
                supplier.getEmail(),
                firstNonBlank(supplier.getBillingAddress(), supplier.getShippingAddress()),
                supplier.getGstin(),
                payment.getPaymentMethod(),
                payment.getReferenceNumber(),
                payment.getAmount(),
                payment.getRemarks(),
                allocations
        );
    }

    @Transactional
    public void sendSalesQuote(Long id, ErpDocumentDtos.SendDocumentRequest request) {
        ErpSalesResponses.SalesQuoteResponse quote = erpSalesService.getQuote(id);
        Customer customer = requireCustomer(quote.organizationId(), quote.customerId());
        sendDocumentEmail(
                resolveRecipient(customer.getEmail(), request),
                request,
                defaultSubject(request.subject(), "Estimate / Quotation " + quote.quoteNumber()),
                defaultMessage(request.message(), "Please find attached the estimate / quotation " + quote.quoteNumber() + "."),
                quote.quoteNumber() + ".pdf",
                generateSalesQuotePdf(id)
        );
    }

    @Transactional
    public void sendSalesOrder(Long id, ErpDocumentDtos.SendDocumentRequest request) {
        ErpSalesResponses.SalesOrderResponse order = erpSalesService.getOrder(id);
        Customer customer = requireCustomer(order.organizationId(), order.customerId());
        sendDocumentEmail(
                resolveRecipient(customer.getEmail(), request),
                request,
                defaultSubject(request.subject(), "Sales Order " + order.orderNumber()),
                defaultMessage(request.message(), "Please find attached the sales order " + order.orderNumber() + "."),
                order.orderNumber() + ".pdf",
                generateSalesOrderPdf(id)
        );
    }

    @Transactional
    public void sendSalesInvoice(Long id, ErpDocumentDtos.SendDocumentRequest request) {
        ErpSalesResponses.SalesInvoiceResponse invoice = erpSalesService.getInvoice(id);
        Customer customer = requireCustomer(invoice.organizationId(), invoice.customerId());
        sendDocumentEmail(
                resolveRecipient(customer.getEmail(), request),
                request,
                defaultSubject(request.subject(), "Invoice " + invoice.invoiceNumber()),
                defaultMessage(request.message(), "Please find attached the invoice " + invoice.invoiceNumber() + "."),
                invoice.invoiceNumber() + ".pdf",
                generateSalesInvoicePdf(id)
        );
    }

    @Transactional
    public void sendCustomerReceipt(Long id, ErpDocumentDtos.SendDocumentRequest request) {
        CustomerReceipt receipt = customerReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer receipt not found: " + id));
        Customer customer = requireCustomer(receipt.getOrganizationId(), receipt.getCustomerId());
        sendDocumentEmail(
                resolveRecipient(customer.getEmail(), request),
                request,
                defaultSubject(request == null ? null : request.subject(), "Customer Receipt " + receipt.getReceiptNumber()),
                defaultMessage(request == null ? null : request.message(), "Please find attached the customer receipt " + receipt.getReceiptNumber() + "."),
                receipt.getReceiptNumber() + ".pdf",
                generateCustomerReceiptPdf(id)
        );
    }

    @Transactional
    public void sendPurchaseOrder(Long id, ErpDocumentDtos.SendDocumentRequest request) {
        ErpPurchaseResponses.PurchaseOrderResponse order = erpPurchaseService.getPurchaseOrder(id);
        Supplier supplier = requireSupplier(order.organizationId(), order.supplierId());
        sendDocumentEmail(
                resolveRecipient(supplier.getEmail(), request),
                request,
                defaultSubject(request.subject(), "Purchase Order " + order.poNumber()),
                defaultMessage(request.message(), "Please find attached the purchase order " + order.poNumber() + "."),
                order.poNumber() + ".pdf",
                generatePurchaseOrderPdf(id)
        );
    }

    @Transactional
    public void sendPurchaseReceipt(Long id, ErpDocumentDtos.SendDocumentRequest request) {
        ErpPurchaseResponses.PurchaseReceiptResponse receipt = erpPurchaseService.getPurchaseReceipt(id);
        Supplier supplier = requireSupplier(receipt.organizationId(), receipt.supplierId());
        sendDocumentEmail(
                resolveRecipient(supplier.getEmail(), request),
                request,
                defaultSubject(request.subject(), "Purchase Receipt " + receipt.receiptNumber()),
                defaultMessage(request.message(), "Please find attached the purchase receipt " + receipt.receiptNumber() + "."),
                receipt.receiptNumber() + ".pdf",
                generatePurchaseReceiptPdf(id)
        );
    }

    @Transactional
    public void sendSupplierPayment(Long id, ErpDocumentDtos.SendDocumentRequest request) {
        SupplierPayment payment = supplierPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier payment not found: " + id));
        Supplier supplier = requireSupplier(payment.getOrganizationId(), payment.getSupplierId());
        sendDocumentEmail(
                resolveRecipient(supplier.getEmail(), request),
                request,
                defaultSubject(request == null ? null : request.subject(), "Supplier Payment " + payment.getPaymentNumber()),
                defaultMessage(request == null ? null : request.message(), "Please find attached the supplier payment " + payment.getPaymentNumber() + "."),
                payment.getPaymentNumber() + ".pdf",
                generateSupplierPaymentPdf(id)
        );
    }

    private byte[] generateSalesDocumentPdf(
            String title,
            String documentNumber,
            LocalDate documentDate,
            LocalDate dueOrValidDate,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long customerId,
            String sellerGstin,
            String customerGstin,
            String placeOfSupplyStateCode,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String remarks,
            List<RenderedLine> lines,
            TaxBreakdown taxBreakdown
    ) {
        Organization organization = requireOrganization(organizationId);
        Branch branch = requireBranch(organizationId, branchId);
        Warehouse warehouse = warehouseId == null ? null : requireWarehouse(organizationId, warehouseId);
        Customer customer = requireCustomer(organizationId, customerId);
        return renderPdf(
                title,
                documentNumber,
                documentDate,
                dueOrValidDate,
                organization,
                branch,
                warehouse,
                customer.getFullName(),
                customer.getLegalName(),
                customer.getPhone(),
                customer.getEmail(),
                firstNonBlank(customer.getBillingAddress(), customer.getShippingAddress()),
                customerGstin,
                placeOfSupplyStateCode,
                sellerGstin,
                subtotal,
                discountAmount,
                taxAmount,
                totalAmount,
                remarks,
                lines,
                taxBreakdown
        );
    }

    private byte[] generatePurchaseDocumentPdf(
            String title,
            String documentNumber,
            LocalDate documentDate,
            LocalDate dueDate,
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long supplierId,
            String sellerGstin,
            String supplierGstin,
            String placeOfSupplyStateCode,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String remarks,
            List<RenderedLine> lines,
            TaxBreakdown taxBreakdown
    ) {
        Organization organization = requireOrganization(organizationId);
        Branch branch = requireBranch(organizationId, branchId);
        Warehouse warehouse = warehouseId == null ? null : requireWarehouse(organizationId, warehouseId);
        Supplier supplier = requireSupplier(organizationId, supplierId);
        return renderPdf(
                title,
                documentNumber,
                documentDate,
                dueDate,
                organization,
                branch,
                warehouse,
                firstNonBlank(supplier.getTradeName(), supplier.getName()),
                supplier.getLegalName(),
                supplier.getPhone(),
                supplier.getEmail(),
                firstNonBlank(supplier.getBillingAddress(), supplier.getShippingAddress()),
                supplierGstin,
                placeOfSupplyStateCode,
                sellerGstin,
                subtotal,
                discountAmount,
                taxAmount,
                totalAmount,
                remarks,
                lines,
                taxBreakdown
        );
    }

    private byte[] renderPdf(
            String title,
            String documentNumber,
            LocalDate documentDate,
            LocalDate dueOrValidDate,
            Organization organization,
            Branch branch,
            Warehouse warehouse,
            String partyDisplayName,
            String partyLegalName,
            String partyPhone,
            String partyEmail,
            String partyAddress,
            String partyGstin,
            String placeOfSupplyStateCode,
            String sellerGstin,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String remarks,
            List<RenderedLine> lines,
            TaxBreakdown taxBreakdown
    ) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            Paragraph header = new Paragraph(title)
                    .setFont(bold)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(header);
            document.add(new Paragraph(organization.getLegalName() != null ? organization.getLegalName() : organization.getName())
                    .setFont(bold)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(joinNonBlank(prefix("Phone: ", organization.getPhone()), prefix("Email: ", organization.getEmail()), prefix("GSTIN: ", sellerGstin)))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10));
            document.add(new Paragraph("\n"));

            Table summary = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            summary.addCell(labelValueCell("Document No", documentNumber));
            summary.addCell(labelValueCell("Date", documentDate == null ? "-" : documentDate.toString()));
            summary.addCell(labelValueCell(dueOrValidDate == null ? "Reference" : "Due / Valid Until", dueOrValidDate == null ? branch.getCode() : dueOrValidDate.toString()));
            summary.addCell(labelValueCell("Branch / Warehouse", warehouse == null ? branch.getName() : branch.getName() + " / " + warehouse.getName()));
            document.add(summary);
            document.add(new Paragraph("\n"));

            Table parties = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            parties.addCell(partyCell("From", organization.getName(), organization.getLegalName(), organization.getPhone(), organization.getEmail(), null));
            parties.addCell(partyCell("To", partyDisplayName, partyLegalName, partyPhone, partyEmail, partyAddress));
            document.add(parties);
            document.add(new Paragraph(joinNonBlank(prefix("Place of Supply: ", placeOfSupplyStateCode), prefix("Counterparty GSTIN: ", partyGstin)))
                    .setFontSize(10));
            document.add(new Paragraph("\n"));

            Table items = new Table(UnitValue.createPercentArray(new float[]{2.3f, 1.1f, 0.9f, 0.8f, 1.0f, 1.0f, 1.1f}))
                    .useAllAvailableWidth();
            addHeader(items, "Item");
            addHeader(items, "SKU/Code");
            addHeader(items, "HSN");
            addHeader(items, "Qty");
            addHeader(items, "UOM");
            addHeader(items, "Unit");
            addHeader(items, "Amount");
            for (RenderedLine line : lines) {
                items.addCell(valueCell(line.itemName()));
                items.addCell(valueCell(line.itemCode()));
                items.addCell(valueCell(line.hsnCode()));
                items.addCell(valueCell(line.quantity()));
                items.addCell(valueCell(line.uomCode()));
                items.addCell(valueCell(line.unitValue()));
                items.addCell(valueCell(line.lineAmount()));
            }
            document.add(items);
            document.add(new Paragraph("\n"));

            Table totals = new Table(UnitValue.createPercentArray(new float[]{2f, 1f})).setWidth(UnitValue.createPercentValue(44)).setMarginLeft(295);
            totals.addCell(labelValueCell("Subtotal", money(subtotal)));
            if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                totals.addCell(labelValueCell("Discount", money(discountAmount)));
            }
            totals.addCell(labelValueCell("Taxable Amount", money(taxBreakdown.taxableAmount())));
            if (taxBreakdown.cgstAmount().compareTo(BigDecimal.ZERO) > 0) {
                totals.addCell(labelValueCell("CGST", money(taxBreakdown.cgstAmount())));
            }
            if (taxBreakdown.sgstAmount().compareTo(BigDecimal.ZERO) > 0) {
                totals.addCell(labelValueCell("SGST", money(taxBreakdown.sgstAmount())));
            }
            if (taxBreakdown.igstAmount().compareTo(BigDecimal.ZERO) > 0) {
                totals.addCell(labelValueCell("IGST", money(taxBreakdown.igstAmount())));
            }
            if (taxBreakdown.cessAmount().compareTo(BigDecimal.ZERO) > 0) {
                totals.addCell(labelValueCell("CESS", money(taxBreakdown.cessAmount())));
            }
            totals.addCell(labelValueCell("Total Tax", money(taxAmount)));
            totals.addCell(labelValueCell("Total", money(totalAmount)));
            document.add(totals);

            if (remarks != null && !remarks.isBlank()) {
                document.add(new Paragraph("\nRemarks / Terms").setFont(bold).setFontSize(11));
                document.add(new Paragraph(remarks).setFontSize(10));
            }

            document.add(new Paragraph("\nContact").setFont(bold).setFontSize(11));
            document.add(new Paragraph(joinNonBlank(branch.getName(), prefix("Phone: ", branch.getPhone()), prefix("Email: ", branch.getEmail()))).setFontSize(10));
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BusinessException("Failed to generate PDF document");
        }
    }

    private byte[] renderPaymentPdf(
            String title,
            String documentNumber,
            LocalDate documentDate,
            Organization organization,
            Branch branch,
            String partyDisplayName,
            String partyLegalName,
            String partyPhone,
            String partyEmail,
            String partyAddress,
            String partyGstin,
            String paymentMethod,
            String referenceNumber,
            BigDecimal amount,
            String remarks,
            List<PaymentAllocationLine> allocations
    ) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            document.add(new Paragraph(title).setFont(bold).setFontSize(18).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(firstNonBlank(organization.getLegalName(), organization.getName()))
                    .setFont(bold)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(joinNonBlank(prefix("Phone: ", organization.getPhone()), prefix("Email: ", organization.getEmail())))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10));
            document.add(new Paragraph("\n"));

            Table summary = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            summary.addCell(labelValueCell("Document No", documentNumber));
            summary.addCell(labelValueCell("Date", documentDate == null ? "-" : documentDate.toString()));
            summary.addCell(labelValueCell("Payment Method", paymentMethod));
            summary.addCell(labelValueCell("Reference No", firstNonBlank(referenceNumber, branch.getCode())));
            summary.addCell(labelValueCell("Branch", branch.getName()));
            summary.addCell(labelValueCell("Amount", money(amount)));
            document.add(summary);
            document.add(new Paragraph("\n"));

            Table parties = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            parties.addCell(partyCell("From", organization.getName(), organization.getLegalName(), organization.getPhone(), organization.getEmail(), null));
            parties.addCell(partyCell("To", partyDisplayName, partyLegalName, partyPhone, partyEmail, partyAddress));
            document.add(parties);
            document.add(new Paragraph(joinNonBlank(prefix("Counterparty GSTIN: ", partyGstin))).setFontSize(10));
            document.add(new Paragraph("\n"));

            Table allocationsTable = new Table(UnitValue.createPercentArray(new float[]{1.4f, 1.1f, 0.9f})).useAllAvailableWidth();
            addHeader(allocationsTable, "Against Document");
            addHeader(allocationsTable, "Date");
            addHeader(allocationsTable, "Allocated");
            if (allocations.isEmpty()) {
                allocationsTable.addCell(valueCell("Unallocated payment / receipt"));
                allocationsTable.addCell(valueCell("-"));
                allocationsTable.addCell(valueCell(money(amount)));
            } else {
                for (PaymentAllocationLine line : allocations) {
                    allocationsTable.addCell(valueCell(line.documentNumber()));
                    allocationsTable.addCell(valueCell(line.documentDate()));
                    allocationsTable.addCell(valueCell(line.allocatedAmount()));
                }
            }
            document.add(allocationsTable);
            document.add(new Paragraph("\n"));

            BigDecimal allocatedAmount = allocations.stream()
                    .map(PaymentAllocationLine::allocatedAmountRaw)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unappliedAmount = amount.subtract(allocatedAmount);

            Table totals = new Table(UnitValue.createPercentArray(new float[]{2f, 1f})).setWidth(UnitValue.createPercentValue(42)).setMarginLeft(310);
            totals.addCell(labelValueCell("Received / Paid", money(amount)));
            if (!allocations.isEmpty()) {
                totals.addCell(labelValueCell("Allocated", money(allocatedAmount)));
                totals.addCell(labelValueCell("Unapplied", money(unappliedAmount)));
            }
            document.add(totals);

            if (remarks != null && !remarks.isBlank()) {
                document.add(new Paragraph("\nRemarks").setFont(bold).setFontSize(11));
                document.add(new Paragraph(remarks).setFontSize(10));
            }

            document.add(new Paragraph("\nContact").setFont(bold).setFontSize(11));
            document.add(new Paragraph(joinNonBlank(branch.getName(), prefix("Phone: ", branch.getPhone()), prefix("Email: ", branch.getEmail()))).setFontSize(10));
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BusinessException("Failed to generate PDF document");
        }
    }

    private List<RenderedLine> enrichSalesLines(Long organizationId, List<ErpSalesResponses.SalesDocumentLineResponse> lines) {
        List<RenderedLine> rendered = new ArrayList<>();
        Map<Long, StoreProduct> storeProducts = loadStoreProducts(lines.stream().map(ErpSalesResponses.SalesDocumentLineResponse::productId).toList());
        Map<Long, Uom> uoms = loadUoms(lines.stream().map(ErpSalesResponses.SalesDocumentLineResponse::uomId).toList());
        for (ErpSalesResponses.SalesDocumentLineResponse line : lines) {
            StoreProduct product = storeProducts.get(line.productId());
            rendered.add(new RenderedLine(
                    product == null ? "Product #" + line.productId() : product.getName(),
                    product == null ? null : product.getSku(),
                    line.hsnCode(),
                    qty(line.quantity()),
                    uomCode(uoms.get(line.uomId())),
                    money(line.unitPrice()),
                    money(line.lineAmount())
            ));
        }
        return rendered;
    }

    private List<RenderedLine> enrichSalesInvoiceLines(Long organizationId, List<ErpSalesResponses.SalesInvoiceLineResponse> lines) {
        List<RenderedLine> rendered = new ArrayList<>();
        Map<Long, StoreProduct> storeProducts = loadStoreProducts(lines.stream().map(ErpSalesResponses.SalesInvoiceLineResponse::productId).toList());
        Map<Long, Uom> uoms = loadUoms(lines.stream().map(ErpSalesResponses.SalesInvoiceLineResponse::uomId).toList());
        for (ErpSalesResponses.SalesInvoiceLineResponse line : lines) {
            StoreProduct product = storeProducts.get(line.productId());
            rendered.add(new RenderedLine(
                    product == null ? "Product #" + line.productId() : product.getName(),
                    product == null ? null : product.getSku(),
                    line.hsnCode(),
                    qty(line.quantity()),
                    uomCode(uoms.get(line.uomId())),
                    money(line.unitPrice()),
                    money(line.lineAmount())
            ));
        }
        return rendered;
    }

    private List<RenderedLine> enrichPurchaseLines(List<ErpPurchaseResponses.PurchaseLineResponse> lines) {
        List<RenderedLine> rendered = new ArrayList<>();
        Map<Long, Uom> uoms = loadUoms(lines.stream().map(ErpPurchaseResponses.PurchaseLineResponse::uomId).toList());
        for (ErpPurchaseResponses.PurchaseLineResponse line : lines) {
            rendered.add(new RenderedLine(
                    firstNonBlank(line.productName(), "Product #" + line.productId()),
                    firstNonBlank(line.supplierProductCode(), line.sku()),
                    line.hsnCode(),
                    qty(line.quantity()),
                    uomCode(uoms.get(line.uomId())),
                    money(line.unitValue()),
                    money(line.lineAmount())
            ));
        }
        return rendered;
    }

    private TaxBreakdown summarizeSalesDocumentTaxes(List<ErpSalesResponses.SalesDocumentLineResponse> lines) {
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal cess = BigDecimal.ZERO;
        for (ErpSalesResponses.SalesDocumentLineResponse line : lines) {
            taxable = taxable.add(zero(line.taxableAmount()));
            cgst = cgst.add(zero(line.cgstAmount()));
            sgst = sgst.add(zero(line.sgstAmount()));
            igst = igst.add(zero(line.igstAmount()));
            cess = cess.add(zero(line.cessAmount()));
        }
        return new TaxBreakdown(taxable, cgst, sgst, igst, cess);
    }

    private TaxBreakdown summarizeSalesInvoiceTaxes(List<ErpSalesResponses.SalesInvoiceLineResponse> lines) {
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal cess = BigDecimal.ZERO;
        for (ErpSalesResponses.SalesInvoiceLineResponse line : lines) {
            taxable = taxable.add(zero(line.taxableAmount()));
            cgst = cgst.add(zero(line.cgstAmount()));
            sgst = sgst.add(zero(line.sgstAmount()));
            igst = igst.add(zero(line.igstAmount()));
            cess = cess.add(zero(line.cessAmount()));
        }
        return new TaxBreakdown(taxable, cgst, sgst, igst, cess);
    }

    private TaxBreakdown summarizePurchaseTaxes(List<ErpPurchaseResponses.PurchaseLineResponse> lines) {
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal cess = BigDecimal.ZERO;
        for (ErpPurchaseResponses.PurchaseLineResponse line : lines) {
            taxable = taxable.add(zero(line.taxableAmount()));
            cgst = cgst.add(zero(line.cgstAmount()));
            sgst = sgst.add(zero(line.sgstAmount()));
            igst = igst.add(zero(line.igstAmount()));
            cess = cess.add(zero(line.cessAmount()));
        }
        return new TaxBreakdown(taxable, cgst, sgst, igst, cess);
    }

    private List<PaymentAllocationLine> enrichCustomerReceiptAllocations(List<CustomerReceiptAllocation> allocations) {
        List<PaymentAllocationLine> lines = new ArrayList<>();
        for (CustomerReceiptAllocation allocation : allocations) {
            SalesInvoice invoice = salesInvoiceRepository.findById(allocation.getSalesInvoiceId()).orElse(null);
            lines.add(new PaymentAllocationLine(
                    invoice == null ? "Invoice #" + allocation.getSalesInvoiceId() : invoice.getInvoiceNumber(),
                    invoice == null || invoice.getInvoiceDate() == null ? "-" : invoice.getInvoiceDate().toString(),
                    money(allocation.getAllocatedAmount()),
                    allocation.getAllocatedAmount()
            ));
        }
        return lines;
    }

    private List<PaymentAllocationLine> enrichSupplierPaymentAllocations(List<SupplierPaymentAllocation> allocations) {
        List<PaymentAllocationLine> lines = new ArrayList<>();
        for (SupplierPaymentAllocation allocation : allocations) {
            PurchaseReceipt receipt = purchaseReceiptRepository.findById(allocation.getPurchaseReceiptId()).orElse(null);
            lines.add(new PaymentAllocationLine(
                    receipt == null ? "Receipt #" + allocation.getPurchaseReceiptId() : receipt.getReceiptNumber(),
                    receipt == null || receipt.getReceiptDate() == null ? "-" : receipt.getReceiptDate().toString(),
                    money(allocation.getAllocatedAmount()),
                    allocation.getAllocatedAmount()
            ));
        }
        return lines;
    }

    private Map<Long, StoreProduct> loadStoreProducts(List<Long> ids) {
        Map<Long, StoreProduct> map = new HashMap<>();
        storeProductRepository.findAllById(ids).forEach(product -> map.put(product.getId(), product));
        return map;
    }

    private Map<Long, Uom> loadUoms(List<Long> ids) {
        Map<Long, Uom> map = new HashMap<>();
        uomRepository.findAllById(ids).forEach(uom -> map.put(uom.getId(), uom));
        return map;
    }

    private void sendDocumentEmail(
            String to,
            ErpDocumentDtos.SendDocumentRequest request,
            String subject,
            String message,
            String fileName,
            byte[] pdf
    ) {
        EmailRequest emailRequest = EmailRequest.builder()
                .to(to)
                .cc(request == null ? null : request.cc())
                .bcc(request == null ? null : request.bcc())
                .subject(subject)
                .content(message.replace("\n", "<br/>"))
                .attachments(List.of(attachment(fileName, pdf)))
                .build();
        emailService.sendEmailWithAttachment(emailRequest);
    }

    private EmailRequest.Attachment attachment(String fileName, byte[] pdf) {
        EmailRequest.Attachment attachment = new EmailRequest.Attachment();
        attachment.setFileName(fileName);
        attachment.setContent(pdf);
        attachment.setContentType("application/pdf");
        return attachment;
    }

    private String resolveRecipient(String fallback, ErpDocumentDtos.SendDocumentRequest request) {
        String to = request != null ? request.to() : null;
        String resolved = (to != null && !to.isBlank()) ? to : fallback;
        if (resolved == null || resolved.isBlank()) {
            throw new BusinessException("Recipient email not available for this document");
        }
        return resolved;
    }

    private String defaultSubject(String requested, String fallback) {
        return requested != null && !requested.isBlank() ? requested : fallback;
    }

    private String defaultMessage(String requested, String fallback) {
        return requested != null && !requested.isBlank() ? requested : fallback;
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
    }

    private Branch requireBranch(Long organizationId, Long branchId) {
        return branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
    }

    private Warehouse requireWarehouse(Long organizationId, Long warehouseId) {
        return warehouseRepository.findByIdAndOrganizationId(warehouseId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
    }

    private Customer requireCustomer(Long organizationId, Long customerId) {
        return customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
    }

    private Supplier requireSupplier(Long organizationId, Long supplierId) {
        return supplierRepository.findByIdAndOrganizationId(supplierId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
    }

    private Cell labelValueCell(String label, String value) {
        return new Cell().add(new Paragraph(label).setBold().setFontSize(9))
                .add(new Paragraph(safe(value)).setFontSize(10));
    }

    private Cell partyCell(String label, String line1, String line2, String phone, String email, String address) {
        Cell cell = new Cell()
                .add(new Paragraph(label).setBold().setFontSize(10))
                .add(new Paragraph(safe(line1)).setFontSize(11));
        addOptionalLine(cell, line2);
        addOptionalLine(cell, phone);
        addOptionalLine(cell, email);
        addOptionalLine(cell, address);
        return cell;
    }

    private void addOptionalLine(Cell cell, String text) {
        if (text != null && !text.isBlank()) {
            cell.add(new Paragraph(text).setFontSize(9));
        }
    }

    private void addHeader(Table table, String value) {
        table.addHeaderCell(new Cell().add(new Paragraph(value).setBold().setFontSize(9)));
    }

    private Cell valueCell(String value) {
        return new Cell().add(new Paragraph(safe(value)).setFontSize(9));
    }

    private String qty(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(BigDecimal value) {
        return value == null ? "-" : "INR " + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String uomCode(Uom uom) {
        return uom == null ? "-" : uom.getCode();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String prefix(String label, String value) {
        return value == null || value.isBlank() ? null : label + value;
    }

    private String joinNonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank() && !value.endsWith("-")) {
                parts.add(value);
            }
        }
        return parts.isEmpty() ? "-" : String.join(" | ", parts);
    }

    private record RenderedLine(
            String itemName,
            String itemCode,
            String hsnCode,
            String quantity,
            String uomCode,
            String unitValue,
            String lineAmount
    ) {}

    private record PaymentAllocationLine(
            String documentNumber,
            String documentDate,
            String allocatedAmount,
            BigDecimal allocatedAmountRaw
    ) {}

    private record TaxBreakdown(
            BigDecimal taxableAmount,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal cessAmount
    ) {}
}
