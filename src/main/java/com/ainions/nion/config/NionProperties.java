package com.ainions.nion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nion")
public record NionProperties(
        Jwt jwt,
        RateLimit rateLimit,
        Llm llm,
        Notification notification
) {
    public record Jwt(String issuer, String secret, long accessTokenMinutes) {}

    public record RateLimit(int requestsPerMinute) {}

    public record Llm(String provider, String model) {}

    public record Notification(String defaultWebhook) {}
}
