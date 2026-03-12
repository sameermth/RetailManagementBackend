package com.retailmanagement.modules.notification.dto.request;

import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import com.retailmanagement.modules.notification.enums.NotificationPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    private Long userId;

    private Long customerId;

    private Long supplierId;

    private Long distributorId;

    private NotificationType type;

    private NotificationChannel channel;

    private NotificationPriority priority;

    private String title;

    private String content;

    private String recipient;

    private String sender;

    private String referenceType;

    private Long referenceId;

    private LocalDateTime scheduledFor;

    private Map<String, Object> data;
}