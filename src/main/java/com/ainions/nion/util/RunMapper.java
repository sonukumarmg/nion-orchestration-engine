package com.ainions.nion.util;

import com.ainions.nion.api.dto.RunResponse;
import com.ainions.nion.domain.OrchestrationRun;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RunMapper {

    public RunResponse toResponse(OrchestrationRun run) {
        return new RunResponse(
                run.getId(),
                run.getStatus().name(),
                run.getIntent(),
                run.getMessageType() == null ? null : run.getMessageType().name(),
                run.getSummary(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getTasks().stream()
                        .map(task -> new RunResponse.TaskSummary(
                                task.getTaskId(),
                                task.getDomain().name(),
                                task.getPriority().name()
                        ))
                        .collect(Collectors.toList())
        );
    }
}
