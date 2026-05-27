package com.ainions.nion.api.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageRequest(
        @NotBlank String source,
        @NotBlank String payload
) {
}
