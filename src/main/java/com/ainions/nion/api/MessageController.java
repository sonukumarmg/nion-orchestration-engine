package com.ainions.nion.api;

import com.ainions.nion.api.dto.MessageRequest;
import com.ainions.nion.api.dto.MessageResponse;
import com.ainions.nion.orchestration.OrchestrationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final OrchestrationService orchestrationService;

    public MessageController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<MessageResponse> ingest(
            @Valid @RequestBody MessageRequest request,
            Authentication authentication
    ) {
        String submittedBy = authentication == null ? "anonymous" : authentication.getName();
        UUID runId = orchestrationService.startRun(request.source(), request.payload(), submittedBy);
        return ResponseEntity.ok(new MessageResponse(runId, "QUEUED"));
    }
}
