package com.retailmanagement.modules.distributor.dto.response;

import com.retailmanagement.modules.distributor.enums.DistributorOrderStatus;
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
public class DistributorOrderResponse {
    private Long id;
    private String orderNumber;
    private Long distributorId;
    private String distributorName;
    private String distributorCode;
    private Long userId;
    private String userName;
    private LocalDateTime orderDate;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime deliveredDate;
    private DistributorOrderStatus status;
    private List<DistributorOrderItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private String paymentStatus;
    private String shippingMethod;
    private String trackingNumber;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}