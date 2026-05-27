package com.ainions.nion;

import com.ainions.nion.domain.PromptTemplate;
import com.ainions.nion.domain.enums.MessageType;
import com.ainions.nion.llm.JsonSchemaService;
import com.ainions.nion.llm.LlmResponse;
import com.ainions.nion.llm.LlmService;
import com.ainions.nion.llm.PromptTemplateService;
import com.ainions.nion.orchestration.L1Planner;
import com.ainions.nion.orchestration.model.L1Plan;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

class L1PlannerTest {

    @Test
    void usesHeuristicPlanWhenLlmUnavailable() {
        PromptTemplateService promptTemplateService = Mockito.mock(PromptTemplateService.class);
        LlmService llmService = Mockito.mock(LlmService.class);
        JsonSchemaService schemaService = Mockito.mock(JsonSchemaService.class);
        PromptTemplate template = new PromptTemplate();
        template.setContent("prompt");
        template.setJsonSchema("{}");
        Mockito.when(promptTemplateService.loadTemplate(anyString(), anyString(), anyString()))
                .thenReturn(template);
        Mockito.when(llmService.generate(anyString(), anyString()))
                .thenReturn(new LlmResponse("{}", true, "mock"));

        L1Planner planner = new L1Planner(promptTemplateService, llmService, schemaService, new ObjectMapper());
        L1Plan plan = planner.plan("Status update on API progress", "context");

        assertThat(plan.messageType()).isEqualTo(MessageType.STATUS_QUERY);
        assertThat(plan.tasks()).isNotEmpty();
    }
}
