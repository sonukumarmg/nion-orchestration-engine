package com.ainions.nion.api.dto;

import java.util.List;

public record KnowledgeEntryRequest(
        String title,
        String content,
        List<String> tags
) {
}
