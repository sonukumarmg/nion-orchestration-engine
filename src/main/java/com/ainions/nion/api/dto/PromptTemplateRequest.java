package com.ainions.nion.api.dto;

public record PromptTemplateRequest(
        String name,
        String version,
        String content,
        String jsonSchema,
        boolean active
) {
}
