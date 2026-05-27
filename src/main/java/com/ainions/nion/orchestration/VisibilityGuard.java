package com.ainions.nion.orchestration;

import com.ainions.nion.domain.enums.L2Domain;
import com.ainions.nion.domain.enums.L3Agent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VisibilityGuard {

    private static final Logger log = LoggerFactory.getLogger(VisibilityGuard.class);

    private final DomainRegistry domainRegistry;

    public VisibilityGuard(DomainRegistry domainRegistry) {
        this.domainRegistry = domainRegistry;
    }

    public List<L3Agent> enforce(L2Domain domain, List<L3Agent> requested) {
        List<L3Agent> allowed = domainRegistry.agentsFor(domain);
        List<L3Agent> filtered = requested.stream()
                .filter(allowed::contains)
                .toList();
        requested.stream()
                .filter(agent -> !allowed.contains(agent))
                .forEach(agent -> log.warn("Visibility violation blocked agent={} domain={}", agent, domain));
        return filtered;
    }
}
