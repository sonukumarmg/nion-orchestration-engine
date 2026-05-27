package com.ainions.nion;

import com.ainions.nion.domain.enums.L2Domain;
import com.ainions.nion.domain.enums.L3Agent;
import com.ainions.nion.orchestration.DomainRegistry;
import com.ainions.nion.orchestration.VisibilityGuard;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisibilityGuardTest {

    @Test
    void enforcesDomainVisibility() {
        DomainRegistry registry = new DomainRegistry();
        VisibilityGuard guard = new VisibilityGuard(registry);
        List<L3Agent> requested = List.of(
                L3Agent.ACTION_ITEM_EXTRACTION,
                L3Agent.QNA
        );
        List<L3Agent> visible = guard.enforce(L2Domain.TRACKING_EXECUTION, requested);
        assertThat(visible).containsOnly(L3Agent.ACTION_ITEM_EXTRACTION);
    }
}
