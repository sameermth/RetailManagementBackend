package com.retailmanagement.modules.notification.service;

import com.retailmanagement.modules.notification.dto.request.SmsRequest;
import com.retailmanagement.modules.notification.dto.response.NotificationResponse;

public interface SmsService {

    NotificationResponse sendSms(SmsRequest request);

    NotificationResponse sendBulkSms(List<String> phoneNumbers, String message);

    NotificationResponse sendTemplateSms(String templateCode, String phoneNumber, Object data);

    String getSmsDeliveryStatus(String messageId);
}