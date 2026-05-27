package com.ainions.nion.orchestration;

import com.ainions.nion.domain.enums.L2Domain;
import com.ainions.nion.domain.enums.L3Agent;
import com.ainions.nion.orchestration.model.AgentResult;
import com.ainions.nion.orchestration.model.DomainResult;
import com.ainions.nion.orchestration.model.PlannedTaskModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class L2Coordinator {

    private static final Logger log = LoggerFactory.getLogger(L2Coordinator.class);

    private final DomainRegistry domainRegistry;
    private final AgentRegistry agentRegistry;
    private final VisibilityGuard visibilityGuard;
    private final L3AgentRunner agentRunner;

    public L2Coordinator(
            DomainRegistry domainRegistry,
            AgentRegistry agentRegistry,
            VisibilityGuard visibilityGuard,
            L3AgentRunner agentRunner
    ) {
        this.domainRegistry = domainRegistry;
        this.agentRegistry = agentRegistry;
        this.visibilityGuard = visibilityGuard;
        this.agentRunner = agentRunner;
    }

    public DomainResult execute(PlannedTaskModel task, String message, String context) {
        L2Domain domain = task.domain();
        List<L3Agent> candidates = domainRegistry.agentsFor(domain);
        List<L3Agent> selected = selectAgents(task, candidates);
        List<L3Agent> visible = visibilityGuard.enforce(domain, selected);
        List<AgentResult> results = new ArrayList<>();
        for (L3Agent agent : visible) {
            results.add(agentRunner.run(agent, message, context));
        }
        String summary = "Executed " + results.size() + " agents for " + domain.name();
        log.info("L2 domain={} taskId={} agents={}", domain, task.taskId(), visible);
        return new DomainResult(domain, task.taskId(), summary, results);
    }

    private List<L3Agent> selectAgents(PlannedTaskModel task, List<L3Agent> candidates) {
        if (task.agentHints() == null || task.agentHints().isEmpty()) {
            return candidates;
        }
        List<L3Agent> selected = new ArrayList<>();
        String hints = String.join(" ", task.agentHints()).toLowerCase(Locale.ROOT);
        for (L3Agent agent : candidates) {
            String agentName = agent.name().toLowerCase(Locale.ROOT);
            String description = agentRegistry.description(agent).toLowerCase(Locale.ROOT);
            if (hints.contains(agentName.replace("_", " ")) || hints.contains(description)) {
                selected.add(agent);
            }
        }
        return selected.isEmpty() ? candidates : selected;
    }
}
