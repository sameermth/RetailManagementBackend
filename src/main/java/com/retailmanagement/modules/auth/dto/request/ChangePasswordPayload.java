package com.retailmanagement.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordPayload {
    @NotBlank
    private String oldPassword;
    @NotBlank
    private String newPassword;
}
