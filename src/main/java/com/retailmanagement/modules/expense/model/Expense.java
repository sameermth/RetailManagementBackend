package com.retailmanagement.modules.expense.model;

import com.retailmanagement.modules.expense.enums.ExpenseStatus;
import com.retailmanagement.modules.expense.enums.PaymentMethod;
import com.retailmanagement.modules.auth.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expenses")
@EntityListeners(AuditingEntityListener.class)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String expenseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false)
    private LocalDateTime expenseDate;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private ExpenseStatus status;

    private String vendor;

    private String vendorInvoiceNumber;

    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String paidTo;

    private String receiptUrl;

    private String attachmentUrl;

    private String notes;

    private Boolean isReimbursable = false;

    private Boolean isBillable = false;

    private Long customerId;

    private Long projectId;

    private LocalDateTime approvedAt;

    private String approvedBy;

    private String rejectionReason;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    private List<ExpenseAttachment> attachments;
}