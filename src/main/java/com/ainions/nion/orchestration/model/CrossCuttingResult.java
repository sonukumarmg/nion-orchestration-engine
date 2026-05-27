package com.ainions.nion.orchestration.model;

import com.ainions.nion.domain.enums.AgentStatus;

public record CrossCuttingResult(
        String name,
        AgentStatus status,
        String output
) {
}
