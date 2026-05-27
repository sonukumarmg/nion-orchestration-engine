package com.ainions.nion.repository;

import com.ainions.nion.domain.PlannedTask;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlannedTaskRepository extends JpaRepository<PlannedTask, UUID> {
}
