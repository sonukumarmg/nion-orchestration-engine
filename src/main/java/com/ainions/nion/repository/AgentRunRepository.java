package com.ainions.nion.repository;

import com.ainions.nion.domain.AgentRun;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {
}
