package com.retailmanagement.modules.notification.service;

import com.retailmanagement.modules.notification.dto.request.EmailRequest;
import com.retailmanagement.modules.notification.dto.response.NotificationResponse;

import java.util.List;

public interface EmailService {

    NotificationResponse sendSimpleEmail(EmailRequest request);

    NotificationResponse sendHtmlEmail(EmailRequest request);

    NotificationResponse sendEmailWithAttachment(EmailRequest request);

    NotificationResponse sendTemplateEmail(String templateCode, String to, String subject, Object data);

    void sendBulkEmails(EmailRequest request, List<String> recipients);
}