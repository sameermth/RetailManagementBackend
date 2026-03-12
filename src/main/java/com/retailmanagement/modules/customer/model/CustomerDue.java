package com.retailmanagement.modules.customer.model;

import com.retailmanagement.modules.customer.enums.DueStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer_dues")
@EntityListeners(AuditingEntityListener.class)
public class CustomerDue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(unique = true, nullable = false)
    private String dueReference;

    private String invoiceNumber;

    private Long saleId;

    private String description;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal originalAmount;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal remainingAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private DueStatus status;

    private Integer reminderCount = 0;

    private LocalDateTime lastReminderSent;

    private LocalDateTime lastPaymentDate;

    private String notes;

    @CreatedDate
    private LocalDateTime createdAt;
}