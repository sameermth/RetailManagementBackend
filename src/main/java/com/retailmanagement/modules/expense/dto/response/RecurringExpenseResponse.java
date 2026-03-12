package com.retailmanagement.modules.expense.dto.response;

import com.retailmanagement.modules.expense.enums.RecurringFrequency;
import com.retailmanagement.modules.expense.enums.ExpenseStatus;
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
public class RecurringExpenseResponse {
    private Long id;
    private String recurringExpenseNumber;
    private Long categoryId;
    private String categoryName;
    private String description;
    private BigDecimal amount;
    private RecurringFrequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer occurrenceCount;
    private Integer occurrencesGenerated;
    private LocalDate nextGenerationDate;
    private String vendor;
    private String paymentMethod;
    private ExpenseStatus status;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}