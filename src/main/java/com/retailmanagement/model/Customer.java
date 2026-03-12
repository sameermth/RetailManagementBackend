package com.retailmanagement.model;

import com.retailmanagement.model.enums.CustomerType;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;
    private String phone;
    private String address;

    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    // Business specific
    private String businessName;
    private String gstNumber;

    // Due tracking
    private Double totalDueAmount = 0.0;
    private LocalDateTime lastDueDate;
    private Boolean dueReminderEnabled = true;
    private Integer reminderFrequencyDays = 7;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<DueReminder> dueReminders = new ArrayList<>();

    @OneToMany(mappedBy = "customer")
    private List<Order> orders = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}