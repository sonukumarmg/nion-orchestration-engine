package com.ainions.nion.llm;

import com.ainions.nion.config.NionProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NionProperties properties;

    public LlmService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper, NionProperties properties) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public LlmResponse generate(String systemPrompt, String userPrompt) {
        String provider = properties.llm().provider();
        if (!"openai".equalsIgnoreCase(provider)) {
            return new LlmResponse("{}", true, "mock");
        }
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return new LlmResponse("{}", true, "mock");
        }
        try {
            String model = System.getenv().getOrDefault("OPENAI_MODEL", properties.llm().model());
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.2
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject("https://api.openai.com/v1/chat/completions", entity, String.class);
            if (response == null) {
                return new LlmResponse("{}", true, "openai");
            }
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText("{}");
            return new LlmResponse(content, false, "openai");
        } catch (Exception ex) {
            log.warn("LLM call failed, falling back to mock: {}", ex.getMessage());
            return new LlmResponse("{}", true, "openai");
        }
    }
}
