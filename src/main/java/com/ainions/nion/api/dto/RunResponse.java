package com.ainions.nion.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RunResponse(
        UUID id,
        String status,
        String intent,
        String messageType,
        String summary,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        List<TaskSummary> tasks
) {
    public record TaskSummary(String taskId, String domain, String priority) {}
}
