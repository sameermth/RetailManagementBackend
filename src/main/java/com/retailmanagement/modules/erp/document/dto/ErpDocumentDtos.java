package com.retailmanagement.modules.erp.document.dto;

import jakarta.validation.constraints.Email;
import java.util.List;

public final class ErpDocumentDtos {
    private ErpDocumentDtos() {}

    public record SendDocumentRequest(
            @Email String to,
            List<@Email String> cc,
            List<@Email String> bcc,
            String subject,
            String message
    ) {}
}
