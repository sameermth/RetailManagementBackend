package com.retailmanagement.modules.notification.service;

import com.retailmanagement.modules.notification.dto.request.EmailRequest;
import com.retailmanagement.modules.notification.dto.request.NotificationRequest;
import com.retailmanagement.modules.notification.dto.request.SmsRequest;
import com.retailmanagement.modules.notification.dto.response.NotificationResponse;
import com.retailmanagement.modules.notification.dto.response.NotificationStatsResponse;
import com.retailmanagement.modules.notification.enums.NotificationStatus;
import com.retailmanagement.modules.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    NotificationResponse sendNotification(NotificationRequest request);

    NotificationResponse sendEmail(EmailRequest request);

    NotificationResponse sendSms(SmsRequest request);

    NotificationResponse sendInAppNotification(Long userId, String title, String content);

    NotificationResponse sendPushNotification(Long userId, String title, String content);

    NotificationResponse sendDueReminder(Long customerId, Long dueId);

    NotificationResponse sendSaleConfirmation(Long saleId);

    NotificationResponse sendPurchaseOrderNotification(Long purchaseId);

    NotificationResponse sendLowStockAlert(Long productId, Integer currentStock);

    NotificationResponse scheduleNotification(NotificationRequest request);

    NotificationResponse getNotificationById(Long id);

    NotificationResponse getNotificationByNotificationId(String notificationId);

    Page<NotificationResponse> getNotificationsByUser(Long userId, Pageable pageable);

    Page<NotificationResponse> getNotificationsByCustomer(Long customerId, Pageable pageable);

    List<NotificationResponse> getUnreadNotificationsByUser(Long userId);

    void markAsRead(Long id);

    void markAllAsRead(Long userId);

    void cancelNotification(Long id);

    void retryFailedNotifications();

    void processPendingNotifications();

    NotificationStatsResponse getNotificationStats(String period);

    Long getUnreadCount(Long userId);

    boolean isNotificationIdUnique(String notificationId);

    NotificationResponse sendEmailNotification(String recipientEmail, String subject, String body);

    NotificationResponse sendNotificationToRole(String role, String title, String message);
}