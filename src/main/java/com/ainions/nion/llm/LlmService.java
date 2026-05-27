package com.ainions.nion.llm;

import com.ainions.nion.config.NionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final ObjectProvider<ChatClient.Builder> builderProvider;
    private final NionProperties properties;

    public LlmService(ObjectProvider<ChatClient.Builder> builderProvider, NionProperties properties) {
        this.builderProvider = builderProvider;
        this.properties = properties;
    }

    public LlmResponse generate(String systemPrompt, String userPrompt) {
        String provider = properties.llm().provider();
        if (!"openai".equalsIgnoreCase(provider)) {
            return new LlmResponse("{}", true, "mock");
        }
        ChatClient.Builder builder = builderProvider.getIfAvailable();
        if (builder == null) {
            return new LlmResponse("{}", true, "mock");
        }
        try {
            ChatClient client = builder.build();
            String content = client.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            return new LlmResponse(content, false, "openai");
        } catch (Exception ex) {
            log.warn("LLM call failed, falling back to mock: {}", ex.getMessage());
            return new LlmResponse("{}", true, "mock");
        }
    }
}
