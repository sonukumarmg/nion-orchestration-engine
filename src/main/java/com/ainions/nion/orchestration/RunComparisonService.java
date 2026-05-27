package com.ainions.nion.orchestration;

import com.ainions.nion.api.dto.RunComparisonResponse;
import com.ainions.nion.domain.OrchestrationRun;
import com.ainions.nion.repository.OrchestrationRunRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RunComparisonService {

    private final OrchestrationRunRepository runRepository;

    public RunComparisonService(OrchestrationRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public RunComparisonResponse compare(UUID leftId, UUID rightId) {
        OrchestrationRun left = runRepository.findById(leftId).orElseThrow();
        OrchestrationRun right = runRepository.findById(rightId).orElseThrow();
        List<String> differences = new ArrayList<>();
        if (left.getMessageType() != right.getMessageType()) {
            differences.add("Message type differs: " + left.getMessageType() + " vs " + right.getMessageType());
        }
        if (left.getTasks().size() != right.getTasks().size()) {
            differences.add("Task count differs: " + left.getTasks().size() + " vs " + right.getTasks().size());
        }
        if (!left.getStatus().equals(right.getStatus())) {
            differences.add("Status differs: " + left.getStatus() + " vs " + right.getStatus());
        }
        return new RunComparisonResponse(leftId.toString(), rightId.toString(), differences);
    }
}
