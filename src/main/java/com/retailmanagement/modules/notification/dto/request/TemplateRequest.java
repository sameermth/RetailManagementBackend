package com.retailmanagement.modules.notification.dto.request;

import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemplateRequest {

    @NotBlank(message = "Template code is required")
    private String templateCode;

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Notification channel is required")
    private NotificationChannel channel;

    private String subject;

    private String content;

    private String contentHtml;

    private String smsContent;

    private String pushTitle;

    private String pushContent;

    private String placeholders;

    private Boolean isActive;
}