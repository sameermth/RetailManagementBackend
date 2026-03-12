package com.retailmanagement.modules.customer.dto.response;

import com.retailmanagement.modules.customer.enums.DueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDueResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String dueReference;
    private String invoiceNumber;
    private Long saleId;
    private String description;
    private LocalDate dueDate;
    private BigDecimal originalAmount;
    private BigDecimal remainingAmount;
    private BigDecimal paidAmount;
    private DueStatus status;
    private Integer reminderCount;
    private LocalDateTime lastReminderSent;
    private LocalDateTime lastPaymentDate;
    private Integer daysOverdue;
    private String urgency;
    private LocalDateTime createdAt;
}