package com.retailmanagement.model;

import com.retailmanagement.model.enums.NotificationType;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "due_reminders")
public class DueReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private Double dueAmount;
    private LocalDateTime dueDate;
    private String description;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private Boolean reminderSent = false;
    private LocalDateTime reminderSentAt;
    private LocalDateTime nextReminderDate;
    private Boolean isResolved = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}