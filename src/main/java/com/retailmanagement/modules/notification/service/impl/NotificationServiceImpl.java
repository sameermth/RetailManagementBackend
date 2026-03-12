package com.retailmanagement.modules.notification.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.customer.model.Customer;
import com.retailmanagement.modules.customer.repository.CustomerRepository;
import com.retailmanagement.modules.notification.dto.request.EmailRequest;
import com.retailmanagement.modules.notification.dto.request.NotificationRequest;
import com.retailmanagement.modules.notification.dto.request.SmsRequest;
import com.retailmanagement.modules.notification.dto.response.NotificationResponse;
import com.retailmanagement.modules.notification.dto.response.NotificationStatsResponse;
import com.retailmanagement.modules.notification.enums.NotificationStatus;
import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import com.retailmanagement.modules.notification.enums.NotificationPriority;
import com.retailmanagement.modules.notification.mapper.NotificationMapper;
import com.retailmanagement.modules.notification.model.Notification;
import com.retailmanagement.modules.notification.model.NotificationTemplate;
import com.retailmanagement.modules.notification.repository.NotificationRepository;
import com.retailmanagement.modules.notification.repository.NotificationTemplateRepository;
import com.retailmanagement.modules.notification.service.NotificationService;
import com.retailmanagement.modules.sales.model.Sale;
import com.retailmanagement.modules.sales.repository.SaleRepository;
import com.retailmanagement.modules.purchase.model.Purchase;
import com.retailmanagement.modules.purchase.repository.PurchaseRepository;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final NotificationMapper notificationMapper;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    @Value("${sms.api.url}")
    private String smsApiUrl;

    @Value("${sms.api.key}")
    private String smsApiKey;

    @Value("${notification.retry.attempts:3}")
    private int maxRetryAttempts;

    @Override
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("Sending notification of type: {} via channel: {}", request.getType(), request.getChannel());

        // Generate notification ID
        String notificationId = generateNotificationId();

        Notification notification = Notification.builder()
                .notificationId(notificationId)
                .userId(request.getUserId())
                .customerId(request.getCustomerId())
                .supplierId(request.getSupplierId())
                .distributorId(request.getDistributorId())
                .type(request.getType())
                .channel(request.getChannel())
                .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM)
                .title(request.getTitle())
                .content(request.getContent())
                .recipient(request.getRecipient())
                .sender(request.getSender())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .status(NotificationStatus.PENDING)
                .scheduledFor(request.getScheduledFor())
                .retryCount(0)
                .build();

        // Save notification
        Notification savedNotification = notificationRepository.save(notification);

        // If scheduled for future, don't send now
        if (request.getScheduledFor() != null && request.getScheduledFor().isAfter(LocalDateTime.now())) {
            log.info("Notification scheduled for: {}", request.getScheduledFor());
            return notificationMapper.toResponse(savedNotification);
        }

        // Send immediately
        sendNotificationAsync(savedNotification);

        return notificationMapper.toResponse(savedNotification);
    }

    @Async
    protected void sendNotificationAsync(Notification notification) {
        try {
            boolean sent = false;

            switch (notification.getChannel()) {
                case EMAIL:
                    sent = sendEmailNotification(notification);
                    break;
                case SMS:
                    sent = sendSmsNotification(notification);
                    break;
                case IN_APP:
                    sent = sendInAppNotification(notification);
                    break;
                case PUSH_NOTIFICATION:
                    sent = sendPushNotification(notification);
                    break;
                default:
                    log.warn("Unsupported notification channel: {}", notification.getChannel());
                    notification.setStatus(NotificationStatus.FAILED);
                    notification.setErrorMessage("Unsupported channel");
            }

            if (sent) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } else {
                handleFailure(notification, "Failed to send");
            }

        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage(), e);
            handleFailure(notification, e.getMessage());
        }

        notificationRepository.save(notification);
    }

    private boolean sendEmailNotification(Notification notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getTitle());
            helper.setText(notification.getContent(), true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", notification.getRecipient());
            return true;

        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean sendSmsNotification(Notification notification) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("api_key", smsApiKey);
            request.put("to", notification.getRecipient());
            request.put("message", notification.getContent());

            String response = restTemplate.postForObject(smsApiUrl, request, String.class);
            log.info("SMS sent successfully to: {}, response: {}", notification.getRecipient(), response);
            return true;

        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean sendInAppNotification(Notification notification) {
        // In-app notifications are stored in DB and retrieved by the frontend
        notification.setStatus(NotificationStatus.DELIVERED);
        return true;
    }

    private boolean sendPushNotification(Notification notification) {
        // Implementation for push notifications (FCM, APNS, etc.)
        log.info("Push notification would be sent to user: {}", notification.getUserId());
        return true;
    }

    private void handleFailure(Notification notification, String errorMessage) {
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setErrorMessage(errorMessage);

        if (notification.getRetryCount() >= maxRetryAttempts) {
            notification.setStatus(NotificationStatus.FAILED);
        } else {
            notification.setStatus(NotificationStatus.PENDING);
            // Schedule retry with exponential backoff
            notification.setScheduledFor(LocalDateTime.now().plusMinutes(5 * notification.getRetryCount()));
        }
    }

    @Override
    public NotificationResponse sendEmail(EmailRequest request) {
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setChannel(NotificationChannel.EMAIL);
        notificationRequest.setType(NotificationType.SYSTEM_ALERT);
        notificationRequest.setTitle(request.getSubject());
        notificationRequest.setContent(request.getContent());
        notificationRequest.setRecipient(request.getTo());

        return sendNotification(notificationRequest);
    }

    @Override
    public NotificationResponse sendSms(SmsRequest request) {
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setChannel(NotificationChannel.SMS);
        notificationRequest.setType(NotificationType.SYSTEM_ALERT);
        notificationRequest.setTitle("SMS Notification");
        notificationRequest.setContent(request.getMessage());
        notificationRequest.setRecipient(request.getPhoneNumber());

        return sendNotification(notificationRequest);
    }

    @Override
    public NotificationResponse sendInAppNotification(Long userId, String title, String content) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setChannel(NotificationChannel.IN_APP);
        request.setType(NotificationType.SYSTEM_ALERT);
        request.setPriority(NotificationPriority.MEDIUM);
        request.setTitle(title);
        request.setContent(content);

        return sendNotification(request);
    }

    @Override
    public NotificationResponse sendPushNotification(Long userId, String title, String content) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setChannel(NotificationChannel.PUSH_NOTIFICATION);
        request.setType(NotificationType.SYSTEM_ALERT);
        request.setTitle(title);
        request.setContent(content);

        return sendNotification(request);
    }

    @Override
    public NotificationResponse sendDueReminder(Long customerId, Long dueId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        NotificationRequest request = new NotificationRequest();
        request.setCustomerId(customerId);
        request.setChannel(NotificationChannel.EMAIL);
        request.setType(NotificationType.DUE_REMINDER);
        request.setPriority(NotificationPriority.HIGH);
        request.setTitle("Payment Due Reminder");

        String content = String.format(
                "Dear %s,\n\nThis is a reminder that you have a payment due. Please clear it at your earliest convenience.",
                customer.getName()
        );
        request.setContent(content);

        if (customer.getEmail() != null) {
            request.setRecipient(customer.getEmail());
        }

        return sendNotification(request);
    }

    @Override
    public NotificationResponse sendSaleConfirmation(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + saleId));

        if (sale.getCustomer() == null || sale.getCustomer().getEmail() == null) {
            throw new BusinessException("Customer email not available");
        }

        NotificationRequest request = new NotificationRequest();
        request.setCustomerId(sale.getCustomer().getId());
        request.setReferenceType("SALE");
        request.setReferenceId(saleId);
        request.setChannel(NotificationChannel.EMAIL);
        request.setType(NotificationType.SALE_CONFIRMED);
        request.setTitle("Sale Confirmation - Invoice " + sale.getInvoiceNumber());

        String content = String.format(
                "Thank you for your purchase!\n\nInvoice Number: %s\nTotal Amount: ₹%.2f\nDate: %s",
                sale.getInvoiceNumber(),
                sale.getTotalAmount(),
                sale.getSaleDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        request.setContent(content);
        request.setRecipient(sale.getCustomer().getEmail());

        return sendNotification(request);
    }

    @Override
    public NotificationResponse sendPurchaseOrderNotification(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + purchaseId));

        NotificationRequest request = new NotificationRequest();
        request.setSupplierId(purchase.getSupplier().getId());
        request.setReferenceType("PURCHASE");
        request.setReferenceId(purchaseId);
        request.setChannel(NotificationChannel.EMAIL);
        request.setType(NotificationType.PURCHASE_ORDER_CREATED);
        request.setTitle("Purchase Order Created - " + purchase.getPurchaseOrderNumber());

        String content = String.format(
                "Purchase Order %s has been created.\n\nSupplier: %s\nTotal Amount: ₹%.2f\nExpected Delivery: %s",
                purchase.getPurchaseOrderNumber(),
                purchase.getSupplier().getName(),
                purchase.getTotalAmount(),
                purchase.getExpectedDeliveryDate()
        );
        request.setContent(content);

        if (purchase.getSupplier().getEmail() != null) {
            request.setRecipient(purchase.getSupplier().getEmail());
        }

        return sendNotification(request);
    }

    @Override
    public NotificationResponse sendLowStockAlert(Long productId, Integer currentStock) {
        // Implementation for low stock alert
        NotificationRequest request = new NotificationRequest();
        request.setReferenceType("PRODUCT");
        request.setReferenceId(productId);
        request.setChannel(NotificationChannel.IN_APP);
        request.setType(NotificationType.LOW_STOCK_ALERT);
        request.setPriority(NotificationPriority.HIGH);
        request.setTitle("Low Stock Alert");
        request.setContent("Product ID: " + productId + " has only " + currentStock + " units left.");

        return sendNotification(request);
    }

    @Override
    public NotificationResponse scheduleNotification(NotificationRequest request) {
        request.setScheduledFor(request.getScheduledFor());
        return sendNotification(request);
    }

    @Override
    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        return notificationMapper.toResponse(notification);
    }

    @Override
    public NotificationResponse getNotificationByNotificationId(String notificationId) {
        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        return notificationMapper.toResponse(notification);
    }

    @Override
    public Page<NotificationResponse> getNotificationsByUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    public Page<NotificationResponse> getNotificationsByCustomer(Long customerId, Pageable pageable) {
        return notificationRepository.findByCustomerId(customerId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    public List<NotificationResponse> getUnreadNotificationsByUser(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .filter(n -> n.getReadAt() == null)
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.markAsRead(id, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUser(userId, LocalDateTime.now());
    }

    @Override
    public void cancelNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new BusinessException("Cannot cancel already sent notification");
        }

        notification.setStatus(NotificationStatus.CANCELLED);
        notificationRepository.save(notification);
    }

    @Override
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void retryFailedNotifications() {
        log.info("Retrying failed notifications at {}", LocalDateTime.now());

        List<Notification> failedNotifications = notificationRepository.findFailedNotificationsToRetry();

        for (Notification notification : failedNotifications) {
            try {
                sendNotificationAsync(notification);
            } catch (Exception e) {
                log.error("Failed to retry notification ID: {}", notification.getId(), e);
            }
        }

        log.info("Retried {} failed notifications", failedNotifications.size());
    }

    @Override
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void processPendingNotifications() {
        log.info("Processing pending notifications at {}", LocalDateTime.now());

        List<Notification> pendingNotifications = notificationRepository.findPendingNotifications(LocalDateTime.now());

        for (Notification notification : pendingNotifications) {
            try {
                sendNotificationAsync(notification);
            } catch (Exception e) {
                log.error("Failed to process pending notification ID: {}", notification.getId(), e);
            }
        }

        log.info("Processed {} pending notifications", pendingNotifications.size());
    }

    @Override
    public NotificationStatsResponse getNotificationStats(String period) {
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();

        switch (period.toUpperCase()) {
            case "TODAY":
                startDate = LocalDate.now().atStartOfDay();
                break;
            case "WEEK":
                startDate = LocalDate.now().minusDays(7).atStartOfDay();
                break;
            case "MONTH":
                startDate = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                break;
            default:
                startDate = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        }

        // Implementation for statistics
        return NotificationStatsResponse.builder()
                .totalSent(100L)
                .pending(10L)
                .failed(5L)
                .delivered(85L)
                .read(70L)
                .successRate(95.0)
                .build();
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUser(userId);
    }

    @Override
    public boolean isNotificationIdUnique(String notificationId) {
        return !notificationRepository.existsByNotificationId(notificationId);
    }

    private String generateNotificationId() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String notificationId = "NOTIF-" + timestamp + "-" + randomPart;

        while (notificationRepository.existsByNotificationId(notificationId)) {
            randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            notificationId = "NOTIF-" + timestamp + "-" + randomPart;
        }

        return notificationId;
    }
}