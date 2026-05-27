package com.ainions.nion.orchestration.model;

import com.ainions.nion.domain.enums.AgentStatus;
import com.ainions.nion.domain.enums.L3Agent;

public record AgentResult(
        L3Agent agent,
        AgentStatus status,
        String output,
        long execTimeMs
) {
}
