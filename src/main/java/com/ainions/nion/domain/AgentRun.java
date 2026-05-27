package com.ainions.nion.domain;

import com.ainions.nion.domain.enums.AgentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "agent_runs")
public class AgentRun extends BaseEntity {

    @Column(nullable = false)
    private String agentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentStatus status;

    @Column(length = 8000)
    private String outputPayload;

    private long execTimeMs;

    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    @ManyToOne
    @JoinColumn(name = "run_id")
    private OrchestrationRun run;

    @ManyToOne
    @JoinColumn(name = "planned_task_id")
    private PlannedTask task;

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public String getOutputPayload() {
        return outputPayload;
    }

    public void setOutputPayload(String outputPayload) {
        this.outputPayload = outputPayload;
    }

    public long getExecTimeMs() {
        return execTimeMs;
    }

    public void setExecTimeMs(long execTimeMs) {
        this.execTimeMs = execTimeMs;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OrchestrationRun getRun() {
        return run;
    }

    public void setRun(OrchestrationRun run) {
        this.run = run;
    }

    public PlannedTask getTask() {
        return task;
    }

    public void setTask(PlannedTask task) {
        this.task = task;
    }
}
