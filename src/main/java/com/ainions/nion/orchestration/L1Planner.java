package com.ainions.nion.orchestration;

import com.ainions.nion.domain.enums.L2Domain;
import com.ainions.nion.domain.enums.MessageType;
import com.ainions.nion.domain.enums.PriorityLevel;
import com.ainions.nion.llm.JsonSchemaService;
import com.ainions.nion.llm.LlmResponse;
import com.ainions.nion.llm.LlmService;
import com.ainions.nion.llm.PromptTemplateService;
import com.ainions.nion.orchestration.model.L1Plan;
import com.ainions.nion.orchestration.model.PlannedTaskModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class L1Planner {

    private static final Logger log = LoggerFactory.getLogger(L1Planner.class);

    private final PromptTemplateService promptTemplateService;
    private final LlmService llmService;
    private final JsonSchemaService jsonSchemaService;
    private final ObjectMapper objectMapper;

    public L1Planner(
            PromptTemplateService promptTemplateService,
            LlmService llmService,
            JsonSchemaService jsonSchemaService,
            ObjectMapper objectMapper
    ) {
        this.promptTemplateService = promptTemplateService;
        this.llmService = llmService;
        this.jsonSchemaService = jsonSchemaService;
        this.objectMapper = objectMapper;
    }

    public L1Plan plan(String message, String context) {
        L1Plan fallback = heuristicPlan(message);
        var template = promptTemplateService.loadTemplate(
                "l1-router",
                "prompts/l1-router.txt",
                "prompts/l1-router.schema.json"
        );
        String systemPrompt = template.getContent();
        String userPrompt = "Context:\n" + context + "\n\nMessage:\n" + message;
        LlmResponse response = llmService.generate(systemPrompt, userPrompt);
        if (response.fallback()) {
            return fallback;
        }
        try {
            JsonNode node = objectMapper.readTree(response.content());
            var errors = jsonSchemaService.validate(template.getJsonSchema(), response.content());
            if (!errors.isEmpty()) {
                log.warn("L1 plan schema validation failed: {}", errors);
                return fallback;
            }
            return parsePlan(node, fallback);
        } catch (Exception ex) {
            log.warn("L1 plan parse failed: {}", ex.getMessage());
            return fallback;
        }
    }

    private L1Plan parsePlan(JsonNode node, L1Plan fallback) {
        if (!node.has("tasks")) {
            return fallback;
        }
        String intent = node.path("intent").asText(fallback.intent());
        MessageType messageType = fallback.messageType();
        try {
            messageType = MessageType.valueOf(node.path("messageType").asText(fallback.messageType().name()));
        } catch (IllegalArgumentException ignored) {
            messageType = fallback.messageType();
        }
        List<String> gaps = new ArrayList<>();
        node.path("gaps").forEach(item -> gaps.add(item.asText()));
        List<PlannedTaskModel> tasks = new ArrayList<>();
        int index = 1;
        for (JsonNode taskNode : node.path("tasks")) {
            L2Domain domain;
            try {
                domain = L2Domain.valueOf(taskNode.path("domain").asText());
            } catch (IllegalArgumentException ex) {
                domain = fallback.tasks().isEmpty() ? L2Domain.TRACKING_EXECUTION : fallback.tasks().get(0).domain();
            }
            PriorityLevel priority;
            try {
                priority = PriorityLevel.valueOf(taskNode.path("priority").asText("MEDIUM"));
            } catch (IllegalArgumentException ex) {
                priority = PriorityLevel.MEDIUM;
            }
            List<String> hints = new ArrayList<>();
            taskNode.path("agentHints").forEach(item -> hints.add(item.asText()));
            tasks.add(new PlannedTaskModel(
                    String.format("TASK-%03d", index++),
                    domain,
                    priority,
                    taskNode.path("rationale").asText(),
                    hints
            ));
        }
        return new L1Plan(intent, messageType, gaps, tasks);
    }

    private L1Plan heuristicPlan(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        List<PlannedTaskModel> tasks = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        MessageType type = MessageType.AMBIGUOUS;

        if (lower.contains("meeting") || lower.contains("transcript")) {
            type = MessageType.MEETING_TRANSCRIPT;
            tasks.add(task(1, L2Domain.TRACKING_EXECUTION, PriorityLevel.MEDIUM, "Extract action items and risks", List.of("action items", "risks")));
            tasks.add(task(2, L2Domain.COMMUNICATION_COLLABORATION, PriorityLevel.MEDIUM, "Summarize meeting and notify stakeholders", List.of("meeting summary")));
            tasks.add(task(3, L2Domain.LEARNING_IMPROVEMENT, PriorityLevel.LOW, "Capture retrospective insights", List.of("improvements")));
        } else if (lower.contains("escalation") || lower.contains("urgent") || lower.contains("p1")) {
            type = MessageType.ESCALATION;
            tasks.add(task(1, L2Domain.COMMUNICATION_COLLABORATION, PriorityLevel.HIGH, "Define escalation plan", List.of("escalation")));
        } else if (lower.contains("feasible") || lower.contains("feasibility") || lower.contains("estimate")) {
            type = MessageType.FEASIBILITY;
            tasks.add(task(1, L2Domain.LEARNING_IMPROVEMENT, PriorityLevel.MEDIUM, "Assess feasibility and capacity", List.of("feasibility")));
            tasks.add(task(2, L2Domain.TRACKING_EXECUTION, PriorityLevel.MEDIUM, "Check progress and constraints", List.of("progress")));
        } else if (lower.contains("decision")) {
            type = MessageType.DECISION_REQUEST;
            tasks.add(task(1, L2Domain.TRACKING_EXECUTION, PriorityLevel.MEDIUM, "Capture decisions and impact", List.of("decision")));
            tasks.add(task(2, L2Domain.COMMUNICATION_COLLABORATION, PriorityLevel.MEDIUM, "Notify stakeholders", List.of("stakeholder")));
        } else if (lower.contains("status") || lower.contains("progress")) {
            type = MessageType.STATUS_QUERY;
            tasks.add(task(1, L2Domain.TRACKING_EXECUTION, PriorityLevel.MEDIUM, "Report progress and deadlines", List.of("status")));
        } else {
            gaps.add("Clarify scope, timeline, and desired output format.");
            tasks.add(task(1, L2Domain.TRACKING_EXECUTION, PriorityLevel.LOW, "Best-effort extraction", List.of("status")));
        }

        String intent = "Analyze project communication for actionable insights";
        return new L1Plan(intent, type, gaps, tasks);
    }

    private PlannedTaskModel task(int index, L2Domain domain, PriorityLevel priority, String rationale, List<String> hints) {
        return new PlannedTaskModel(String.format("TASK-%03d", index), domain, priority, rationale, hints);
    }
}
