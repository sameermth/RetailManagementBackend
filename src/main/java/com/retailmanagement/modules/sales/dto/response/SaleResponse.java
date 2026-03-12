package com.retailmanagement.modules.sales.dto.response;

import com.retailmanagement.modules.sales.enums.SaleStatus;
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
public class SaleResponse {
    private Long id;
    private String invoiceNumber;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long userId;
    private String userName;
    private LocalDateTime saleDate;
    private List<SaleItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private SaleStatus status;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime dueDate;
    private String notes;
    private String billingAddress;
    private String shippingAddress;
    private Boolean isReturned;
    private LocalDateTime createdAt;
}