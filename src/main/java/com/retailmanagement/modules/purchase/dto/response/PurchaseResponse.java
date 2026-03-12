package com.retailmanagement.modules.purchase.dto.response;

import com.retailmanagement.modules.purchase.enums.PurchaseStatus;
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
public class PurchaseResponse {
    private Long id;
    private String purchaseOrderNumber;
    private Long supplierId;
    private String supplierName;
    private String supplierCode;
    private Long userId;
    private String userName;
    private LocalDateTime orderDate;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime receivedDate;
    private PurchaseStatus status;
    private List<PurchaseItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private String paymentStatus;
    private String paymentTerms;
    private String shippingMethod;
    private String trackingNumber;
    private String invoiceNumber;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}