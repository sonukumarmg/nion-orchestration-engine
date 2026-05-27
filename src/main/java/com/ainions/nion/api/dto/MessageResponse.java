package com.ainions.nion.api.dto;

import java.util.UUID;

public record MessageResponse(
        UUID runId,
        String status
) {
}
