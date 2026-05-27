package com.ainions.nion.api;

import com.ainions.nion.api.dto.RunComparisonResponse;
import com.ainions.nion.api.dto.RunResponse;
import com.ainions.nion.orchestration.RunComparisonService;
import com.ainions.nion.repository.OrchestrationRunRepository;
import com.ainions.nion.util.RunMapper;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runs")
public class RunController {

    private final OrchestrationRunRepository runRepository;
    private final RunMapper runMapper;
    private final RunComparisonService comparisonService;

    public RunController(
            OrchestrationRunRepository runRepository,
            RunMapper runMapper,
            RunComparisonService comparisonService
    ) {
        this.runRepository = runRepository;
        this.runMapper = runMapper;
        this.comparisonService = comparisonService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','VIEWER')")
    public ResponseEntity<RunResponse> getRun(@PathVariable UUID id) {
        return runRepository.findById(id)
                .map(runMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/map")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','VIEWER')")
    public ResponseEntity<String> getMap(@PathVariable UUID id) {
        return runRepository.findById(id)
                .map(run -> ResponseEntity.ok(run.getMapOutput()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/compare")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<RunComparisonResponse> compare(@RequestParam UUID left, @RequestParam UUID right) {
        return ResponseEntity.ok(comparisonService.compare(left, right));
    }
}
