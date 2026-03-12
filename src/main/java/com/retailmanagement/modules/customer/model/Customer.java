package com.retailmanagement.modules.customer.model;

import com.retailmanagement.modules.customer.enums.CustomerType;
import com.retailmanagement.modules.customer.enums.CustomerStatus;
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
@Table(name = "customers")
@EntityListeners(AuditingEntityListener.class)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String customerCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    @Enumerated(EnumType.STRING)
    private CustomerStatus status;

    private String email;

    private String phone;

    private String alternatePhone;

    @Column(length = 500)
    private String address;

    private String city;

    private String state;

    private String country;

    private String pincode;

    private String gstNumber;

    private String panNumber;

    private String website;

    // Business specific fields
    private String businessName;
    private String contactPerson;
    private String designation;

    @Column(precision = 10, scale = 2)
    private BigDecimal creditLimit;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalDueAmount = BigDecimal.ZERO;

    private Integer paymentTerms; // in days

    private Boolean dueReminderEnabled = true;

    private Integer reminderFrequencyDays = 7;

    private LocalDateTime lastReminderSent;

    private LocalDateTime lastDueDate;

    // Loyalty fields
    private Integer loyaltyPoints = 0;

    private String loyaltyTier; // BRONZE, SILVER, GOLD, PLATINUM

    @Column(precision = 10, scale = 2)
    private BigDecimal totalPurchaseAmount = BigDecimal.ZERO;

    private LocalDateTime lastPurchaseDate;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<CustomerDue> dues = new ArrayList<>();

    @OneToMany(mappedBy = "customer")
    private List<com.retailmanagement.modules.sales.model.Sale> sales = new ArrayList<>();

    private String notes;

    private String profileImageUrl;

    private Boolean isActive = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}