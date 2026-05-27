package com.ainions.nion.notification;

import com.ainions.nion.config.NionProperties;
import com.ainions.nion.domain.OrchestrationRun;
import com.ainions.nion.orchestration.model.CrossCuttingResult;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RestTemplate restTemplate;
    private final NionProperties properties;

    public NotificationService(RestTemplateBuilder restTemplateBuilder, NionProperties properties) {
        this.restTemplate = restTemplateBuilder.build();
        this.properties = properties;
    }

    public void notifyCompletion(OrchestrationRun run, CrossCuttingResult evaluation) {
        String webhook = properties.notification().defaultWebhook();
        if (webhook == null || webhook.isBlank()) {
            return;
        }
        Map<String, Object> payload = Map.of(
                "runId", run.getId().toString(),
                "status", run.getStatus().name(),
                "messageType", run.getMessageType() == null ? "UNKNOWN" : run.getMessageType().name(),
                "quality", evaluation.status().name()
        );
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(webhook, payload, Void.class);
            log.info("Webhook delivered status={} runId={}", response.getStatusCode(), run.getId());
        } catch (Exception ex) {
            log.warn("Webhook delivery failed runId={} error={}", run.getId(), ex.getMessage());
        }
    }
}
