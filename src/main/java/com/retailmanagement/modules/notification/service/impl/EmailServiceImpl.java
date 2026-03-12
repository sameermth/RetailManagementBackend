package com.retailmanagement.modules.notification.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.modules.notification.dto.request.EmailRequest;
import com.retailmanagement.modules.notification.dto.response.NotificationResponse;
import com.retailmanagement.modules.notification.model.NotificationTemplate;
import com.retailmanagement.modules.notification.repository.NotificationTemplateRepository;
import com.retailmanagement.modules.notification.service.EmailService;
import com.retailmanagement.modules.notification.service.NotificationService;
import com.retailmanagement.common.utils.TemplateParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final NotificationService notificationService;
    private final NotificationTemplateRepository templateRepository;
    private final TemplateParser templateParser;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public NotificationResponse sendSimpleEmail(EmailRequest request) {
        log.info("Sending simple email to: {}", request.getTo());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(request.getTo());

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }

            helper.setSubject(request.getSubject());
            helper.setText(request.getContent(), false);

            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", request.getTo());

            // Create notification record
            return createNotificationRecord(request, "SENT");

        } catch (Exception e) {
            log.error("Failed to send simple email: {}", e.getMessage(), e);
            return createNotificationRecord(request, "FAILED");
        }
    }

    @Override
    public NotificationResponse sendHtmlEmail(EmailRequest request) {
        log.info("Sending HTML email to: {}", request.getTo());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(request.getTo());

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }

            helper.setSubject(request.getSubject());
            helper.setText(request.getContent(), true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", request.getTo());

            return createNotificationRecord(request, "SENT");

        } catch (Exception e) {
            log.error("Failed to send HTML email: {}", e.getMessage(), e);
            return createNotificationRecord(request, "FAILED");
        }
    }

    @Override
    public NotificationResponse sendEmailWithAttachment(EmailRequest request) {
        log.info("Sending email with attachment to: {}", request.getTo());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(request.getTo());

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }

            helper.setSubject(request.getSubject());
            helper.setText(request.getContent(), true);

            // Add attachments
            if (request.getAttachments() != null) {
                for (EmailRequest.Attachment attachment : request.getAttachments()) {
                    helper.addAttachment(attachment.getFileName(),
                            new ByteArrayResource(attachment.getContent()));
                }
            }

            mailSender.send(message);
            log.info("Email with attachment sent successfully to: {}", request.getTo());

            return createNotificationRecord(request, "SENT");

        } catch (Exception e) {
            log.error("Failed to send email with attachment: {}", e.getMessage(), e);
            return createNotificationRecord(request, "FAILED");
        }
    }

    @Override
    public NotificationResponse sendTemplateEmail(String templateCode, String to, String subject, Object data) {
        log.info("Sending template email to: {} using template: {}", to, templateCode);

        NotificationTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new BusinessException("Email template not found: " + templateCode));

        // Parse template with data
        String parsedContent = templateParser.parse(template.getContentHtml(), data);

        EmailRequest request = new EmailRequest();
        request.setTo(to);
        request.setSubject(subject != null ? subject : template.getSubject());
        request.setContent(parsedContent);
        request.setTemplateCode(templateCode);
        request.setTemplateData((Map<String, Object>) data);

        return sendHtmlEmail(request);
    }

    @Override
    @Async
    public void sendBulkEmails(EmailRequest request, List<String> recipients) {
        log.info("Sending bulk emails to {} recipients", recipients.size());

        for (String recipient : recipients) {
            try {
                EmailRequest bulkRequest = new EmailRequest();
                bulkRequest.setTo(recipient);
                bulkRequest.setSubject(request.getSubject());
                bulkRequest.setContent(request.getContent());
                bulkRequest.setCc(request.getCc());
                bulkRequest.setBcc(request.getBcc());
                bulkRequest.setAttachments(request.getAttachments());

                sendHtmlEmail(bulkRequest);

                // Add small delay to avoid overwhelming the mail server
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Failed to send email to: {}", recipient, e);
            }
        }
    }

    private NotificationResponse createNotificationRecord(EmailRequest request, String status) {
        com.retailmanagement.modules.notification.dto.request.NotificationRequest notifRequest =
                new com.retailmanagement.modules.notification.dto.request.NotificationRequest();

        notifRequest.setChannel(com.retailmanagement.modules.notification.enums.NotificationChannel.EMAIL);
        notifRequest.setType(com.retailmanagement.modules.notification.enums.NotificationType.SYSTEM_ALERT);
        notifRequest.setTitle(request.getSubject());
        notifRequest.setContent(request.getContent());
        notifRequest.setRecipient(request.getTo());

        return notificationService.sendNotification(notifRequest);
    }

    // Helper class for attachment handling
    private static class ByteArrayResource extends org.springframework.core.io.ByteArrayResource {
        private final String filename;

        public ByteArrayResource(byte[] byteArray) {
            super(byteArray);
            this.filename = "attachment";
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}