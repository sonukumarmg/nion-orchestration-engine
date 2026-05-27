package com.ainions.nion.api.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String tenantId
) {
}
