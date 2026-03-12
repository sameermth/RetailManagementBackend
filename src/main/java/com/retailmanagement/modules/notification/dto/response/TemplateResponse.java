package com.retailmanagement.modules.notification.dto.response;

import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {
    private Long id;
    private String templateCode;
    private String name;
    private String description;
    private NotificationType type;
    private NotificationChannel channel;
    private String subject;
    private String content;
    private String contentHtml;
    private String smsContent;
    private String pushTitle;
    private String pushContent;
    private String placeholders;
    private Boolean isActive;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}