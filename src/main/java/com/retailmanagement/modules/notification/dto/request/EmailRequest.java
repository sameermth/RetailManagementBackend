package com.retailmanagement.modules.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;

    private List<String> cc;

    private List<String> bcc;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    private String templateCode;

    private Map<String, Object> templateData;

    private List<Attachment> attachments;

    @Data
    public static class Attachment {
        private String fileName;
        private byte[] content;
        private String contentType;
    }
}