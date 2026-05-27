package com.ainions.nion.orchestration.model;

import com.ainions.nion.domain.enums.L2Domain;
import java.util.List;

public record DomainResult(
        L2Domain domain,
        String taskId,
        String summary,
        List<AgentResult> agentResults
) {
}
