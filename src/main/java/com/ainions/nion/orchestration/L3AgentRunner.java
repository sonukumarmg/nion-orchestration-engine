package com.ainions.nion.orchestration;

import com.ainions.nion.domain.enums.AgentStatus;
import com.ainions.nion.domain.enums.L3Agent;
import com.ainions.nion.llm.JsonSchemaService;
import com.ainions.nion.llm.LlmResponse;
import com.ainions.nion.llm.LlmService;
import com.ainions.nion.llm.PromptTemplateService;
import com.ainions.nion.orchestration.model.AgentResult;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class L3AgentRunner {

    private static final Logger log = LoggerFactory.getLogger(L3AgentRunner.class);

    private final PromptTemplateService promptTemplateService;
    private final LlmService llmService;
    private final JsonSchemaService jsonSchemaService;

    public L3AgentRunner(
            PromptTemplateService promptTemplateService,
            LlmService llmService,
            JsonSchemaService jsonSchemaService
    ) {
        this.promptTemplateService = promptTemplateService;
        this.llmService = llmService;
        this.jsonSchemaService = jsonSchemaService;
    }

    public AgentResult run(L3Agent agent, String message, String context) {
        Instant start = Instant.now();
        String promptPath = "prompts/l3/" + agent.name().toLowerCase() + ".txt";
        var template = promptTemplateService.loadTemplate(
                "l3." + agent.name().toLowerCase(),
                promptPath,
                "prompts/l3/default.schema.json"
        );
        String userPrompt = "Context:\n" + context + "\n\nMessage:\n" + message;
        LlmResponse response = llmService.generate(template.getContent(), userPrompt);
        String output = response.content();
        AgentStatus status = AgentStatus.SUCCESS;
        if (response.fallback() || !jsonSchemaService.validate(template.getJsonSchema(), output).isEmpty()) {
            status = AgentStatus.PARTIAL;
        }
        long duration = Duration.between(start, Instant.now()).toMillis();
        log.info("L3 agent {} status={} durationMs={}", agent, status, duration);
        return new AgentResult(agent, status, output, duration);
    }
}
