package com.retailmanagement.modules.expense.model;

import com.retailmanagement.modules.expense.enums.RecurringFrequency;
import com.retailmanagement.modules.expense.enums.ExpenseStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recurring_expenses")
@EntityListeners(AuditingEntityListener.class)
public class RecurringExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String recurringExpenseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    private String description;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private RecurringFrequency frequency;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer occurrenceCount;

    private Integer occurrencesGenerated = 0;

    private LocalDate nextGenerationDate;

    private String vendor;

    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    private ExpenseStatus status;

    private String notes;

    private Boolean isActive = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}