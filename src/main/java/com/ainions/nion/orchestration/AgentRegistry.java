package com.ainions.nion.orchestration;

import com.ainions.nion.domain.enums.L3Agent;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AgentRegistry {

    private static final Map<L3Agent, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry(L3Agent.ACTION_ITEM_EXTRACTION, "Extracts clear tasks with owners and deadlines"),
            Map.entry(L3Agent.RISK_TRACKING, "Identifies risks and suggests mitigation"),
            Map.entry(L3Agent.DECISION_TRACKING, "Captures decisions and rationales"),
            Map.entry(L3Agent.PROGRESS_MONITORING, "Summarizes current progress"),
            Map.entry(L3Agent.DEADLINE_MANAGEMENT, "Flags deadlines and timeline risk"),
            Map.entry(L3Agent.MESSAGE_DELIVERY, "Drafts delivery-ready messages"),
            Map.entry(L3Agent.STAKEHOLDER_NOTIFICATION, "Creates stakeholder updates"),
            Map.entry(L3Agent.MEETING_SUMMARY, "Produces meeting minutes"),
            Map.entry(L3Agent.ESCALATION_HANDLER, "Defines escalation path"),
            Map.entry(L3Agent.QNA, "Answers questions from context"),
            Map.entry(L3Agent.RETROSPECTIVE_ANALYSIS, "Finds retro insights"),
            Map.entry(L3Agent.PATTERN_RECOGNITION, "Detects recurring patterns"),
            Map.entry(L3Agent.KNOWLEDGE_BASE_UPDATE, "Suggests knowledge base updates"),
            Map.entry(L3Agent.IMPROVEMENT_SUGGESTIONS, "Recommends improvements"),
            Map.entry(L3Agent.KNOWLEDGE_RETRIEVAL, "Retrieves project context"),
            Map.entry(L3Agent.EVALUATION, "Evaluates output quality")
    );

    public String description(L3Agent agent) {
        return DESCRIPTIONS.getOrDefault(agent, agent.name());
    }
}
