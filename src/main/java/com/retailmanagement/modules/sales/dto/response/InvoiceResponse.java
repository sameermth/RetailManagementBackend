package com.retailmanagement.modules.sales.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private Long saleId;
    private LocalDateTime invoiceDate;
    private LocalDateTime dueDate;
    private String customerName;
    private String customerAddress;
    private String customerGst;
    private String customerPhone;
    private String customerEmail;
    private List<InvoiceItemDTO> items;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private String paymentTerms;
    private String notes;
    private String termsAndConditions;
    private String bankDetails;
    private String pdfUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceItemDTO {
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal tax;
        private BigDecimal total;
    }
}