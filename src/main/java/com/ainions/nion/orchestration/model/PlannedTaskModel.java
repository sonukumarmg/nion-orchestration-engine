package com.ainions.nion.orchestration.model;

import com.ainions.nion.domain.enums.L2Domain;
import com.ainions.nion.domain.enums.PriorityLevel;
import java.util.List;

public record PlannedTaskModel(
        String taskId,
        L2Domain domain,
        PriorityLevel priority,
        String rationale,
        List<String> agentHints
) {
}
