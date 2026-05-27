package com.ainions.nion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        @Email String email,
        @NotBlank String displayName,
        String tenantId
) {
}
