package com.retailmanagement.modules.notification.dto.response;

import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import com.retailmanagement.modules.notification.enums.NotificationStatus;
import com.retailmanagement.modules.notification.enums.NotificationPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String notificationId;
    private Long userId;
    private String userName;
    private Long customerId;
    private String customerName;
    private Long supplierId;
    private String supplierName;
    private Long distributorId;
    private String distributorName;
    private NotificationType type;
    private NotificationChannel channel;
    private NotificationStatus status;
    private NotificationPriority priority;
    private String title;
    private String content;
    private String recipient;
    private String sender;
    private String referenceType;
    private Long referenceId;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime scheduledFor;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime createdAt;
}