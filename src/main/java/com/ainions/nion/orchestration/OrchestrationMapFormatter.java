package com.ainions.nion.orchestration;

import com.ainions.nion.domain.enums.AgentStatus;
import com.ainions.nion.orchestration.model.DomainResult;
import com.ainions.nion.orchestration.model.L1Plan;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationMapFormatter {

    public String format(String runId, L1Plan plan, List<DomainResult> domainResults) {
        StringBuilder builder = new StringBuilder();
        builder.append("╔══════════════════════════════════════════════════════════════════════╗\n");
        builder.append("║      NION ORCHESTRATION MAP  ·  Spring Orchestration Engine        ║\n");
        builder.append("╚══════════════════════════════════════════════════════════════════════╝\n");
        builder.append("  Message ID  :  ").append(runId).append("\n");
        builder.append("  Timestamp   :  ").append(OffsetDateTime.now()).append("\n\n");
        builder.append("────────────────────────────────────────────────────────────────────────\n");
        builder.append("  📋  L1 PLAN\n");
        builder.append("────────────────────────────────────────────────────────────────────────\n");
        builder.append("  Intent        :  ").append(plan.intent()).append("\n");
        builder.append("  Message Type  :  ").append(plan.messageType()).append("\n");
        builder.append("  Gaps          :  ").append(plan.gaps().isEmpty() ? "None" : String.join(", ", plan.gaps())).append("\n\n");
        builder.append("  Routing Plan:\n\n");
        plan.tasks().forEach(task -> builder.append("  [")
                .append(task.taskId())
                .append("]  ──▶  ")
                .append(task.domain())
                .append("\n    │  Rationale  :  ")
                .append(task.rationale())
                .append("\n    │  Priority   :  ")
                .append(task.priority())
                .append("\n    └  Hints      :  ")
                .append(String.join(", ", task.agentHints()))
                .append("\n\n"));
        builder.append("────────────────────────────────────────────────────────────────────────\n");
        builder.append("  ⚙️   L2 / L3 EXECUTION\n");
        builder.append("────────────────────────────────────────────────────────────────────────\n\n");
        for (DomainResult domainResult : domainResults) {
            builder.append("  ┌── [")
                    .append(domainResult.taskId())
                    .append("]  ")
                    .append(domainResult.domain())
                    .append("\n  │   Summary  :  ")
                    .append(domainResult.summary())
                    .append("\n  │   Agents   :  ")
                    .append(domainResult.agentResults().stream().map(result -> result.agent().name()).toList())
                    .append("\n");
            domainResult.agentResults().forEach(result -> builder.append("  ├──▶ [")
                    .append(result.status() == AgentStatus.SUCCESS ? "✓" : "⚠")
                    .append("] ")
                    .append(result.agent().name())
                    .append(" ( ")
                    .append(result.execTimeMs())
                    .append("ms )\n"));
            builder.append("\n");
        }
        builder.append("════════════════════════════════════════════════════════════════════════\n");
        builder.append("  ✅  NION ORCHESTRATION MAP COMPLETE\n");
        builder.append("════════════════════════════════════════════════════════════════════════\n");
        return builder.toString();
    }
}
