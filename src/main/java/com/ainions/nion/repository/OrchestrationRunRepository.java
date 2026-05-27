package com.ainions.nion.repository;

import com.ainions.nion.domain.OrchestrationRun;
import com.ainions.nion.domain.enums.RunStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestrationRunRepository extends JpaRepository<OrchestrationRun, UUID> {
    List<OrchestrationRun> findByStatus(RunStatus status);
}
