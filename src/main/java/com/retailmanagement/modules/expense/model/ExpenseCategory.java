package com.retailmanagement.modules.expense.model;

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
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expense_categories")
@EntityListeners(AuditingEntityListener.class)
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String categoryCode;

    @Column(nullable = false)
    private String name;

    private String description;

    private String type; // OPERATIONAL, ADMINISTRATIVE, MARKETING, UTILITY, SALARY, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private ExpenseCategory parentCategory;

    @OneToMany(mappedBy = "parentCategory")
    private List<ExpenseCategory> subCategories = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    private List<Expense> expenses = new ArrayList<>();

    @Column(precision = 10, scale = 2)
    private BigDecimal budgetAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    private Boolean isActive = true;

    private String notes;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}