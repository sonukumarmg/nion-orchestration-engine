package com.ainions.nion.orchestration;

import com.ainions.nion.domain.enums.AgentStatus;
import com.ainions.nion.llm.JsonSchemaService;
import com.ainions.nion.llm.LlmResponse;
import com.ainions.nion.llm.LlmService;
import com.ainions.nion.llm.PromptTemplateService;
import com.ainions.nion.orchestration.model.CrossCuttingResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {

    private final PromptTemplateService promptTemplateService;
    private final LlmService llmService;
    private final JsonSchemaService jsonSchemaService;

    public EvaluationService(
            PromptTemplateService promptTemplateService,
            LlmService llmService,
            JsonSchemaService jsonSchemaService
    ) {
        this.promptTemplateService = promptTemplateService;
        this.llmService = llmService;
        this.jsonSchemaService = jsonSchemaService;
    }

    public CrossCuttingResult evaluate(String message, List<String> summaries) {
        var template = promptTemplateService.loadTemplate(
                "evaluation",
                "prompts/evaluation.txt",
                "prompts/evaluation.schema.json"
        );
        String userPrompt = "Message:\n" + message + "\n\nSummaries:\n" + String.join("\n", summaries);
        LlmResponse response = llmService.generate(template.getContent(), userPrompt);
        AgentStatus status = response.fallback() ? AgentStatus.PARTIAL : AgentStatus.SUCCESS;
        if (!jsonSchemaService.validate(template.getJsonSchema(), response.content()).isEmpty()) {
            status = AgentStatus.PARTIAL;
        }
        return new CrossCuttingResult("evaluation", status, response.content());
    }
}
