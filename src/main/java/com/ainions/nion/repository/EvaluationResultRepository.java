package com.ainions.nion.repository;

import com.ainions.nion.domain.EvaluationResult;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, UUID> {
}
