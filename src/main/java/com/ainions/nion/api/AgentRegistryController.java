package com.ainions.nion.api;

import com.ainions.nion.domain.enums.L2Domain;
import com.ainions.nion.domain.enums.L3Agent;
import com.ainions.nion.orchestration.DomainRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentRegistryController {

    private final DomainRegistry domainRegistry;

    public AgentRegistryController(DomainRegistry domainRegistry) {
        this.domainRegistry = domainRegistry;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','VIEWER')")
    public ResponseEntity<Map<String, List<L3Agent>>> listAgents() {
        Map<String, List<L3Agent>> response = new LinkedHashMap<>();
        for (L2Domain domain : L2Domain.values()) {
            response.put(domain.name(), domainRegistry.agentsFor(domain));
        }
        response.put("CROSS_CUTTING", domainRegistry.crossCuttingAgents());
        return ResponseEntity.ok(response);
    }
}
