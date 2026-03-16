package com.retailmanagement.modules.notification.model;

import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import com.retailmanagement.modules.notification.enums.NotificationStatus;
import com.retailmanagement.modules.notification.enums.NotificationPriority;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String notificationId;

    private Long userId;

    private Long customerId;

    private Long supplierId;

    private Long distributorId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    private NotificationPriority priority;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String content;

    private String recipient; // Email address, phone number, etc.

    private String sender;

    private String referenceType; // SALE, PURCHASE, DUE, etc.

    private Long referenceId;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    private LocalDateTime scheduledFor;

    private Integer retryCount = 0;

    private String errorMessage;

    //private String metadata; // JSON string for additional data

    @CreatedDate
    private LocalDateTime createdAt;
}