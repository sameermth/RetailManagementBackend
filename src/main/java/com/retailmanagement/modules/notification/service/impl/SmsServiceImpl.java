package com.retailmanagement.modules.notification.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.modules.notification.dto.request.SmsRequest;
import com.retailmanagement.modules.notification.dto.response.NotificationResponse;
import com.retailmanagement.modules.notification.model.NotificationTemplate;
import com.retailmanagement.modules.notification.repository.NotificationTemplateRepository;
import com.retailmanagement.modules.notification.service.NotificationService;
import com.retailmanagement.modules.notification.service.SmsService;
import com.retailmanagement.common.utils.TemplateParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final RestTemplate restTemplate;
    private final NotificationService notificationService;
    private final NotificationTemplateRepository templateRepository;
    private final TemplateParser templateParser;

    @Value("${sms.api.url}")
    private String smsApiUrl;

    @Value("${sms.api.key}")
    private String smsApiKey;

    @Value("${sms.sender.id}")
    private String senderId;

    @Override
    public NotificationResponse sendSms(SmsRequest request) {
        log.info("Sending SMS to: {}", request.getPhoneNumber());

        try {
            // Prepare API request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + smsApiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("to", request.getPhoneNumber());
            payload.put("message", request.getMessage());
            payload.put("sender", request.getSenderId() != null ? request.getSenderId() : senderId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // Call SMS API
            ResponseEntity<Map> response = restTemplate.exchange(
                    smsApiUrl + "/send",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("SMS sent successfully to: {}, messageId: {}",
                        request.getPhoneNumber(), response.getBody().get("messageId"));

                // Create notification record
                return createNotificationRecord(request, "SENT");
            } else {
                log.error("Failed to send SMS. Response: {}", response);
                return createNotificationRecord(request, "FAILED");
            }

        } catch (Exception e) {
            log.error("Error sending SMS: {}", e.getMessage(), e);
            return createNotificationRecord(request, "FAILED");
        }
    }

    @Override
    public NotificationResponse sendBulkSms(List<String> phoneNumbers, String message) {
        log.info("Sending bulk SMS to {} numbers", phoneNumbers.size());

        SmsRequest request = new SmsRequest();
        request.setMessage(message);

        NotificationResponse lastResponse = null;

        for (String phoneNumber : phoneNumbers) {
            try {
                request.setPhoneNumber(phoneNumber);
                lastResponse = sendSms(request);

                // Add small delay to avoid rate limiting
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Failed to send SMS to: {}", phoneNumber, e);
            }
        }

        return lastResponse;
    }

    @Override
    public NotificationResponse sendTemplateSms(String templateCode, String phoneNumber, Object data) {
        log.info("Sending template SMS to: {} using template: {}", phoneNumber, templateCode);

        NotificationTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new BusinessException("SMS template not found: " + templateCode));

        // Parse template with data
        String parsedMessage = templateParser.parse(template.getSmsContent(), data);

        SmsRequest request = new SmsRequest();
        request.setPhoneNumber(phoneNumber);
        request.setMessage(parsedMessage);
        request.setTemplateCode(templateCode);

        return sendSms(request);
    }

    @Override
    public String getSmsDeliveryStatus(String messageId) {
        log.info("Checking delivery status for messageId: {}", messageId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + smsApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    smsApiUrl + "/status/" + messageId,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {
                return response.getBody().get("status").toString();
            }

        } catch (Exception e) {
            log.error("Failed to get delivery status: {}", e.getMessage(), e);
        }

        return "UNKNOWN";
    }

    private NotificationResponse createNotificationRecord(SmsRequest request, String status) {
        com.retailmanagement.modules.notification.dto.request.NotificationRequest notifRequest =
                new com.retailmanagement.modules.notification.dto.request.NotificationRequest();

        notifRequest.setChannel(com.retailmanagement.modules.notification.enums.NotificationChannel.SMS);
        notifRequest.setType(com.retailmanagement.modules.notification.enums.NotificationType.SYSTEM_ALERT);
        notifRequest.setTitle("SMS Notification");
        notifRequest.setContent(request.getMessage());
        notifRequest.setRecipient(request.getPhoneNumber());

        return notificationService.sendNotification(notifRequest);
    }
}