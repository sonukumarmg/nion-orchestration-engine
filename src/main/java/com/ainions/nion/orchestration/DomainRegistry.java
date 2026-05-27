package com.ainions.nion.orchestration;

import com.ainions.nion.domain.enums.L2Domain;
import com.ainions.nion.domain.enums.L3Agent;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DomainRegistry {

    private static final Map<L2Domain, List<L3Agent>> DOMAIN_AGENTS = Map.of(
            L2Domain.TRACKING_EXECUTION, List.of(
                    L3Agent.ACTION_ITEM_EXTRACTION,
                    L3Agent.RISK_TRACKING,
                    L3Agent.DECISION_TRACKING,
                    L3Agent.PROGRESS_MONITORING,
                    L3Agent.DEADLINE_MANAGEMENT
            ),
            L2Domain.COMMUNICATION_COLLABORATION, List.of(
                    L3Agent.MESSAGE_DELIVERY,
                    L3Agent.STAKEHOLDER_NOTIFICATION,
                    L3Agent.MEETING_SUMMARY,
                    L3Agent.ESCALATION_HANDLER,
                    L3Agent.QNA
            ),
            L2Domain.LEARNING_IMPROVEMENT, List.of(
                    L3Agent.RETROSPECTIVE_ANALYSIS,
                    L3Agent.PATTERN_RECOGNITION,
                    L3Agent.KNOWLEDGE_BASE_UPDATE,
                    L3Agent.IMPROVEMENT_SUGGESTIONS
            )
    );

    private static final List<L3Agent> CROSS_CUTTING = List.of(
            L3Agent.KNOWLEDGE_RETRIEVAL,
            L3Agent.EVALUATION
    );

    public List<L3Agent> agentsFor(L2Domain domain) {
        return DOMAIN_AGENTS.getOrDefault(domain, List.of());
    }

    public List<L3Agent> crossCuttingAgents() {
        return CROSS_CUTTING;
    }

    public Map<L2Domain, List<L3Agent>> domainAgents() {
        return DOMAIN_AGENTS;
    }
}
