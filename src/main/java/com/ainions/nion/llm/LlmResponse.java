package com.ainions.nion.llm;

public record LlmResponse(String content, boolean fallback, String provider) {
}
